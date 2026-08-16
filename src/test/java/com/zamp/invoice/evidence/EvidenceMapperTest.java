package com.zamp.invoice.evidence;

import com.zamp.invoice.model.entity.ExtractionEvidence;
import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.model.extraction.OcrWord;
import com.zamp.invoice.model.llm.LlmInvoiceResult;
import com.zamp.invoice.repository.ExtractionEvidenceRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/*
 * Note on the fourth required scenario ("PDF_TEXT invoice — evidence mapper not called"):
 * that guard lives in ExtractionPipelineService, not in EvidenceMapper itself —
 * the pipeline only invokes evidenceMapper.map(...) inside an
 * `if (result.extractionMethod() == ExtractionMethod.OCR)` block. EvidenceMapper has no
 * awareness of extraction method and nothing to unit test for the PDF_TEXT case; it simply
 * is never called. This is documented here rather than covered by a test in this class.
 */
class EvidenceMapperTest {

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final ExtractionEvidenceRepository extractionEvidenceRepository = mock(ExtractionEvidenceRepository.class);
    private final EvidenceMapper evidenceMapper = new EvidenceMapper(invoiceRepository, extractionEvidenceRepository);

    private final UUID invoiceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(invoiceRepository.getReferenceById(any())).thenReturn(Invoice.builder().id(invoiceId).build());
    }

    @Test
    void singleInvoiceLevelFieldMatchedExactly() {
        List<OcrWord> ocrWords = List.of(
                new OcrWord("297.00", 0.91f, new Rectangle(0, 0, 10, 10))
        );
        LlmInvoiceResult llmResult = LlmInvoiceResult.builder()
                .totalAmount(new BigDecimal("297.00"))
                .build();

        List<ExtractionEvidence> evidence = evidenceMapper.map(invoiceId, llmResult, ocrWords, List.of());

        assertThat(evidence).hasSize(1);
        assertThat(evidence.get(0).getFieldName()).isEqualTo(FieldName.TOTAL_AMOUNT);
        assertThat(evidence.get(0).getOcrConfidence()).isEqualByComparingTo(new BigDecimal("0.91"));
    }

    @Test
    void multiWordVendorNameTakesMinimumConfidence() {
        List<OcrWord> ocrWords = List.of(
                new OcrWord("Acme", 0.96f, new Rectangle(0, 0, 10, 10)),
                new OcrWord("Supplies", 0.82f, new Rectangle(20, 0, 10, 10))
        );
        LlmInvoiceResult llmResult = LlmInvoiceResult.builder()
                .vendorName("Acme Supplies")
                .build();

        List<ExtractionEvidence> evidence = evidenceMapper.map(invoiceId, llmResult, ocrWords, List.of());

        assertThat(evidence).hasSize(1);
        assertThat(evidence.get(0).getFieldName()).isEqualTo(FieldName.VENDOR_NAME);
        assertThat(evidence.get(0).getOcrConfidence()).isEqualByComparingTo(new BigDecimal("0.82"));
    }

    @Test
    void noMatchFoundStillCreatesEvidenceRowWithNullConfidence() {
        List<OcrWord> ocrWords = List.of(
                new OcrWord("hello", 0.90f, new Rectangle(0, 0, 10, 10))
        );
        LlmInvoiceResult llmResult = LlmInvoiceResult.builder()
                .totalAmount(new BigDecimal("500.00"))
                .build();

        List<ExtractionEvidence> evidence = evidenceMapper.map(invoiceId, llmResult, ocrWords, List.of());

        assertThat(evidence).hasSize(1);
        assertThat(evidence.get(0).getFieldName()).isEqualTo(FieldName.TOTAL_AMOUNT);
        assertThat(evidence.get(0).getOcrConfidence()).isNull();
    }

    @Test
    void lineItemFieldMatchedWithBoundingBoxDisambiguation() {
        UUID lineItemId = UUID.randomUUID();
        InvoiceLineItem savedLineItem = InvoiceLineItem.builder()
                .id(lineItemId)
                .lineNumber(1)
                .build();

        List<OcrWord> ocrWords = List.of(
                new OcrWord("Widget", 0.95f, new Rectangle(0, 95, 10, 10)),
                new OcrWord("125", 0.80f, new Rectangle(0, 100, 10, 10)),
                new OcrWord("125", 0.70f, new Rectangle(0, 200, 10, 10))
        );

        LlmInvoiceResult.LlmLineItem llmLineItem = LlmInvoiceResult.LlmLineItem.builder()
                .lineNumber(1)
                .description("Widget")
                .amount(new BigDecimal("125"))
                .build();
        LlmInvoiceResult llmResult = LlmInvoiceResult.builder()
                .lineItems(List.of(llmLineItem))
                .build();

        List<ExtractionEvidence> evidence = evidenceMapper.map(invoiceId, llmResult, ocrWords, List.of(savedLineItem));

        assertThat(evidence).hasSize(2);

        ExtractionEvidence descriptionEvidence = evidence.stream()
                .filter(e -> e.getFieldName() == FieldName.DESCRIPTION)
                .findFirst().orElseThrow();
        assertThat(descriptionEvidence.getLineItemId()).isEqualTo(lineItemId);
        assertThat(descriptionEvidence.getOcrConfidence()).isEqualByComparingTo(new BigDecimal("0.95"));

        ExtractionEvidence amountEvidence = evidence.stream()
                .filter(e -> e.getFieldName() == FieldName.AMOUNT)
                .findFirst().orElseThrow();
        assertThat(amountEvidence.getLineItemId()).isEqualTo(lineItemId);
        // Two "125" candidates: y=100 (close to the prior matched "Widget" at y=95) and y=200.
        // The y=100 word (confidence 0.80) must be chosen, not the y=200 word (confidence 0.70).
        assertThat(amountEvidence.getOcrConfidence()).isEqualByComparingTo(new BigDecimal("0.80"));
    }
}
