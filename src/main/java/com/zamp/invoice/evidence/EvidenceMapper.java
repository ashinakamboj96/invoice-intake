package com.zamp.invoice.evidence;

import com.zamp.invoice.model.entity.ExtractionEvidence;
import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.model.extraction.OcrWord;
import com.zamp.invoice.model.llm.LlmInvoiceResult;
import com.zamp.invoice.repository.ExtractionEvidenceRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.awt.Rectangle;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bridges the LLM's structured output back to Tesseract's OCR results: for each field the LLM
 * extracted, finds the OCR word(s) it most likely came from and records that word's confidence as
 * the field's trust signal. This is the one place OCR confidence and LLM output actually meet —
 * see decisions.md ("Evidence-based confidence") for why that connection matters.
 */
@Component
public class EvidenceMapper {

    private final InvoiceRepository invoiceRepository;
    private final ExtractionEvidenceRepository extractionEvidenceRepository;

    public EvidenceMapper(InvoiceRepository invoiceRepository, ExtractionEvidenceRepository extractionEvidenceRepository) {
        this.invoiceRepository = invoiceRepository;
        this.extractionEvidenceRepository = extractionEvidenceRepository;
    }

    /**
     * Connects each LLM-extracted field value back to the OCR word(s) that produced it, so its
     * OCR confidence can be tracked per field. Persists one {@link ExtractionEvidence} row per
     * non-null invoice-level and line-item field — with a null {@code ocrConfidence} when no
     * matching OCR word could be found, rather than omitting the row.
     *
     * @param invoiceId      the invoice these evidence rows belong to
     * @param llmResult      the structured fields extracted by the LLM
     * @param ocrWords       every word Tesseract found on the document, with position/confidence
     * @param savedLineItems the already-persisted line items, matched to {@code llmResult}'s
     *                       line items by line number
     * @return the persisted evidence rows
     */
    public List<ExtractionEvidence> map(UUID invoiceId,
                                         LlmInvoiceResult llmResult,
                                         List<OcrWord> ocrWords,
                                         List<InvoiceLineItem> savedLineItems) {
        List<NormalizedWord> normalizedWords = ocrWords.stream()
                .map(w -> new NormalizedWord(normalize(w.getText()), w.getConfidence(), w.getBoundingBox()))
                .toList();

        List<ExtractionEvidence> evidence = new ArrayList<>();
        evidence.addAll(mapInvoiceLevelFields(invoiceId, llmResult, normalizedWords));
        evidence.addAll(mapLineItemFields(invoiceId, llmResult, normalizedWords, savedLineItems));

        extractionEvidenceRepository.saveAll(evidence);
        return evidence;
    }

    private String normalize(String text) {
        return text.toLowerCase().replaceAll("[₹$€£¥,]", "").trim();
    }

    @Data
    @AllArgsConstructor
    private static class NormalizedWord {
        private String normalized;
        private float confidence;
        private Rectangle boundingBox;
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

        return fields.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> buildEvidence(invoiceId, null, entry.getKey(),
                        matchInvoiceLevelValue(entry.getValue(), normalizedWords)))
                .toList();
    }

    /*
     * Kept as an explicit loop rather than a stream: matchedBoxesForLineItem accumulates
     * across iterations of the inner field loop, and each field's bounding-box disambiguation
     * depends on the boxes matched by previously-processed fields for the same line item. That
     * sequential, order-dependent mutation doesn't have a clean declarative stream equivalent.
     */
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
                    confidence = toBigDecimal(chosen.getConfidence());
                    matchedBoxesForLineItem.add(chosen.getBoundingBox());
                }
                evidence.add(buildEvidence(invoiceId, savedLineItem.getId(), entry.getKey(), confidence));
            }
        }

        return evidence;
    }

    private BigDecimal matchInvoiceLevelValue(Object value, List<NormalizedWord> normalizedWords) {
        String normalizedValue = normalize(fieldValueToString(value));

        return Arrays.stream(normalizedValue.split("\\s+"))
                .filter(token -> !token.isBlank())
                .map(token -> findBestMatch(token, normalizedWords))
                .filter(Objects::nonNull)
                .map(this::toBigDecimal)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private Float findBestMatch(String token, List<NormalizedWord> normalizedWords) {
        return normalizedWords.stream()
                .filter(w -> w.getNormalized().equals(token))
                .findFirst()
                .or(() -> normalizedWords.stream().filter(w -> w.getNormalized().contains(token)).findFirst())
                .map(NormalizedWord::getConfidence)
                .orElse(null);
    }

    private NormalizedWord matchLineItemValue(Object value,
                                               List<NormalizedWord> normalizedWords,
                                               List<Rectangle> matchedBoxesForLineItem) {
        String token = normalize(fieldValueToString(value));

        List<NormalizedWord> candidates = normalizedWords.stream()
                .filter(w -> w.getNormalized().equals(token) || w.getNormalized().contains(token))
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1 || matchedBoxesForLineItem.isEmpty()) {
            return candidates.get(0);
        }

        return candidates.stream()
                .min(Comparator.comparingInt(candidate -> matchedBoxesForLineItem.stream()
                        .mapToInt(box -> Math.abs(candidate.getBoundingBox().y - box.y))
                        .min()
                        .orElse(Integer.MAX_VALUE)))
                .orElse(null);
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
