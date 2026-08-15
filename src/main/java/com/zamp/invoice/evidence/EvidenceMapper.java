package com.zamp.invoice.evidence;

import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.FieldName;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.extraction.OcrWord;
import com.zamp.invoice.llm.LlmInvoiceResult;
import com.zamp.invoice.repository.ExtractionEvidenceRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.awt.Rectangle;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EvidenceMapper {

    private final InvoiceRepository invoiceRepository;
    private final ExtractionEvidenceRepository extractionEvidenceRepository;

    public EvidenceMapper(InvoiceRepository invoiceRepository, ExtractionEvidenceRepository extractionEvidenceRepository) {
        this.invoiceRepository = invoiceRepository;
        this.extractionEvidenceRepository = extractionEvidenceRepository;
    }

    public List<ExtractionEvidence> map(UUID invoiceId,
                                         LlmInvoiceResult llmResult,
                                         List<OcrWord> ocrWords,
                                         List<InvoiceLineItem> savedLineItems) {
        List<NormalizedWord> normalizedWords = ocrWords.stream()
                .map(w -> new NormalizedWord(normalize(w.text()), w.confidence(), w.boundingBox()))
                .collect(Collectors.toList());

        List<ExtractionEvidence> evidence = new ArrayList<>();
        evidence.addAll(mapInvoiceLevelFields(invoiceId, llmResult, normalizedWords));
        evidence.addAll(mapLineItemFields(invoiceId, llmResult, normalizedWords, savedLineItems));

        extractionEvidenceRepository.saveAll(evidence);
        return evidence;
    }

    private String normalize(String text) {
        return text.toLowerCase().replaceAll("[₹$€£¥,]", "").trim();
    }

    private record NormalizedWord(String normalized, float confidence, Rectangle boundingBox) {
    }

    private List<ExtractionEvidence> mapInvoiceLevelFields(UUID invoiceId,
                                                             LlmInvoiceResult llmResult,
                                                             List<NormalizedWord> normalizedWords) {
        Map<FieldName, Object> fields = new LinkedHashMap<>();
        fields.put(FieldName.VENDOR_NAME, llmResult.getVendorName());
        fields.put(FieldName.INVOICE_NUMBER, llmResult.getInvoiceNumber());
        fields.put(FieldName.INVOICE_DATE, llmResult.getInvoiceDate());
        fields.put(FieldName.CURRENCY, llmResult.getCurrency());
        fields.put(FieldName.SUBTOTAL_AMOUNT, llmResult.getSubtotalAmount());
        fields.put(FieldName.TAX_AMOUNT, llmResult.getTaxAmount());
        fields.put(FieldName.TOTAL_AMOUNT, llmResult.getTotalAmount());

        List<ExtractionEvidence> evidence = new ArrayList<>();
        for (Map.Entry<FieldName, Object> entry : fields.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            BigDecimal confidence = matchInvoiceLevelValue(entry.getValue(), normalizedWords);
            evidence.add(buildEvidence(invoiceId, null, entry.getKey(), confidence));
        }
        return evidence;
    }

    private List<ExtractionEvidence> mapLineItemFields(UUID invoiceId,
                                                         LlmInvoiceResult llmResult,
                                                         List<NormalizedWord> normalizedWords,
                                                         List<InvoiceLineItem> savedLineItems) {
        List<ExtractionEvidence> evidence = new ArrayList<>();
        if (llmResult.getLineItems() == null) {
            return evidence;
        }

        for (LlmInvoiceResult.LlmLineItem llmLineItem : llmResult.getLineItems()) {
            InvoiceLineItem savedLineItem = savedLineItems.stream()
                    .filter(li -> li.getLineNumber().equals(llmLineItem.getLineNumber()))
                    .findFirst()
                    .orElse(null);
            if (savedLineItem == null) {
                continue;
            }

            List<Rectangle> matchedBoxesForLineItem = new ArrayList<>();

            Map<FieldName, Object> fields = new LinkedHashMap<>();
            fields.put(FieldName.DESCRIPTION, llmLineItem.getDescription());
            fields.put(FieldName.QUANTITY, llmLineItem.getQuantity());
            fields.put(FieldName.UNIT_PRICE, llmLineItem.getUnitPrice());
            fields.put(FieldName.AMOUNT, llmLineItem.getAmount());

            for (Map.Entry<FieldName, Object> entry : fields.entrySet()) {
                if (entry.getValue() == null) {
                    continue;
                }
                NormalizedWord chosen = matchLineItemValue(entry.getValue(), normalizedWords, matchedBoxesForLineItem);
                BigDecimal confidence = null;
                if (chosen != null) {
                    confidence = toBigDecimal(chosen.confidence());
                    matchedBoxesForLineItem.add(chosen.boundingBox());
                }
                evidence.add(buildEvidence(invoiceId, savedLineItem.getId(), entry.getKey(), confidence));
            }
        }

        return evidence;
    }

    private BigDecimal matchInvoiceLevelValue(Object value, List<NormalizedWord> normalizedWords) {
        String normalizedValue = normalize(fieldValueToString(value));
        String[] tokens = normalizedValue.split("\\s+");

        BigDecimal minConfidence = null;
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            Float confidence = findBestMatch(token, normalizedWords);
            if (confidence != null) {
                BigDecimal candidate = toBigDecimal(confidence);
                if (minConfidence == null || candidate.compareTo(minConfidence) < 0) {
                    minConfidence = candidate;
                }
            }
        }
        return minConfidence;
    }

    private Float findBestMatch(String token, List<NormalizedWord> normalizedWords) {
        for (NormalizedWord word : normalizedWords) {
            if (word.normalized().equals(token)) {
                return word.confidence();
            }
        }
        for (NormalizedWord word : normalizedWords) {
            if (word.normalized().contains(token)) {
                return word.confidence();
            }
        }
        return null;
    }

    private NormalizedWord matchLineItemValue(Object value,
                                               List<NormalizedWord> normalizedWords,
                                               List<Rectangle> matchedBoxesForLineItem) {
        String token = normalize(fieldValueToString(value));

        List<NormalizedWord> candidates = normalizedWords.stream()
                .filter(w -> w.normalized().equals(token) || w.normalized().contains(token))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1 || matchedBoxesForLineItem.isEmpty()) {
            return candidates.get(0);
        }

        NormalizedWord best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (NormalizedWord candidate : candidates) {
            for (Rectangle matchedBox : matchedBoxesForLineItem) {
                int distance = Math.abs(candidate.boundingBox().y - matchedBox.y);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = candidate;
                }
            }
        }
        return best;
    }

    private String fieldValueToString(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd.toPlainString();
        }
        return value.toString();
    }

    private BigDecimal toBigDecimal(float confidence) {
        return new BigDecimal(Float.toString(confidence));
    }

    private ExtractionEvidence buildEvidence(UUID invoiceId, UUID lineItemId, FieldName fieldName, BigDecimal confidence) {
        return ExtractionEvidence.builder()
                .id(UUID.randomUUID())
                .invoice(invoiceRepository.getReferenceById(invoiceId))
                .lineItemId(lineItemId)
                .fieldName(fieldName)
                .ocrConfidence(confidence)
                .build();
    }
}
