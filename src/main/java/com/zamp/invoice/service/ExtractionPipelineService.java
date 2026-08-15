package com.zamp.invoice.service;

import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.ExtractionMethod;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.evidence.EvidenceMapper;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.exception.LlmUnavailableException;
import com.zamp.invoice.extraction.DocumentTypeDetector;
import com.zamp.invoice.extraction.ExtractionResult;
import com.zamp.invoice.extraction.OcrExtractor;
import com.zamp.invoice.extraction.PdfTextExtractor;
import com.zamp.invoice.llm.LlmInvoiceResult;
import com.zamp.invoice.llm.LlmStructurer;
import com.zamp.invoice.repository.InvoiceLineItemRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import com.zamp.invoice.validation.ValidationEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
public class ExtractionPipelineService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final DocumentTypeDetector documentTypeDetector;
    private final PdfTextExtractor pdfTextExtractor;
    private final OcrExtractor ocrExtractor;
    private final LlmStructurer llmStructurer;
    private final InvoicePersister invoicePersister;
    private final EvidenceMapper evidenceMapper;
    private final ValidationEngine validationEngine;

    public ExtractionPipelineService(InvoiceRepository invoiceRepository,
                                      InvoiceLineItemRepository invoiceLineItemRepository,
                                      DocumentTypeDetector documentTypeDetector,
                                      PdfTextExtractor pdfTextExtractor,
                                      OcrExtractor ocrExtractor,
                                      LlmStructurer llmStructurer,
                                      InvoicePersister invoicePersister,
                                      EvidenceMapper evidenceMapper,
                                      ValidationEngine validationEngine) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.documentTypeDetector = documentTypeDetector;
        this.pdfTextExtractor = pdfTextExtractor;
        this.ocrExtractor = ocrExtractor;
        this.llmStructurer = llmStructurer;
        this.invoicePersister = invoicePersister;
        this.evidenceMapper = evidenceMapper;
        this.validationEngine = validationEngine;
    }

    @Async("extractionTaskExecutor")
    public void process(UUID invoiceId, byte[] fileBytes, String filename) {
        log.info("[invoiceId={}] EXTRACTION started", invoiceId);
        long startedAt = System.currentTimeMillis();

        try {
            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

            ExtractionMethod method = documentTypeDetector.detect(fileBytes);
            log.info("[invoiceId={}] DETECTION method={}", invoiceId, method);

            ExtractionResult result = method == ExtractionMethod.PDF_TEXT
                    ? new ExtractionResult(pdfTextExtractor.extract(fileBytes), null, ExtractionMethod.PDF_TEXT)
                    : ocrExtractor.extract(fileBytes, filename);

            invoice.setExtractionMethod(result.getExtractionMethod());
            invoiceRepository.save(invoice);

            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("[invoiceId={}] EXTRACTION completed method={} textLength={} duration={}ms",
                    invoiceId, result.getExtractionMethod(), result.getRawText().length(), durationMs);

            try {
                log.info("[invoiceId={}] LLM_STRUCTURING started", invoiceId);
                long llmStartedAt = System.currentTimeMillis();

                LlmInvoiceResult llmResult = llmStructurer.structure(result.getRawText());
                invoicePersister.persist(invoiceId, llmResult);

                long llmDurationMs = System.currentTimeMillis() - llmStartedAt;
                log.info("[invoiceId={}] LLM_STRUCTURING completed duration={}ms fields_extracted={}",
                        invoiceId, llmDurationMs, countExtractedFields(llmResult));

                if (result.getExtractionMethod() == ExtractionMethod.OCR) {
                    List<InvoiceLineItem> savedLineItems = invoiceLineItemRepository.findByInvoiceId(invoiceId);
                    List<ExtractionEvidence> evidence = evidenceMapper.map(invoiceId, llmResult, result.getWords(), savedLineItems);
                    log.info("[invoiceId={}] EVIDENCE_MAPPING matched={} fields", invoiceId, evidence.size());
                }

                log.info("[invoiceId={}] VALIDATION started", invoiceId);
                validationEngine.validate(invoiceId);
                Invoice validatedInvoice = invoiceRepository.findById(invoiceId)
                        .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
                log.info("[invoiceId={}] VALIDATION completed status={}", invoiceId, validatedInvoice.getStatus());
            } catch (LlmUnavailableException e) {
                log.error("[invoiceId={}] LLM_STRUCTURING failed reason={}", invoiceId, e.getMessage());
                markFailed(invoiceId, "LLM structuring failed: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("[invoiceId={}] EXTRACTION failed reason={}", invoiceId, e.getMessage(), e);
            markFailed(invoiceId, e.getMessage());
        }
    }

    private long countExtractedFields(LlmInvoiceResult result) {
        return Stream.of(
                        result.getVendorName(), result.getInvoiceNumber(), result.getInvoiceDate(),
                        result.getCurrency(), result.getSubtotalAmount(), result.getTaxAmount(), result.getTotalAmount())
                .filter(Objects::nonNull)
                .count();
    }

    private void markFailed(UUID invoiceId, String message) {
        invoiceRepository.findById(invoiceId)
                .ifPresent(invoice -> {
                    invoice.setStatus(InvoiceStatus.FAILED);
                    invoice.setFailureMessage(message);
                    invoiceRepository.save(invoice);
                });
    }
}
