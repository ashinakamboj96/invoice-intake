package com.zamp.invoice.service;

import com.zamp.invoice.domain.ExtractionMethod;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.exception.ExtractionFailedException;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.extraction.DocumentTypeDetector;
import com.zamp.invoice.extraction.ExtractionResult;
import com.zamp.invoice.extraction.OcrExtractor;
import com.zamp.invoice.extraction.PdfTextExtractor;
import com.zamp.invoice.repository.InvoiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class ExtractionPipelineService {

    private final InvoiceRepository invoiceRepository;
    private final DocumentTypeDetector documentTypeDetector;
    private final PdfTextExtractor pdfTextExtractor;
    private final OcrExtractor ocrExtractor;

    public ExtractionPipelineService(InvoiceRepository invoiceRepository,
                                      DocumentTypeDetector documentTypeDetector,
                                      PdfTextExtractor pdfTextExtractor,
                                      OcrExtractor ocrExtractor) {
        this.invoiceRepository = invoiceRepository;
        this.documentTypeDetector = documentTypeDetector;
        this.pdfTextExtractor = pdfTextExtractor;
        this.ocrExtractor = ocrExtractor;
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

            invoice.setExtractionMethod(result.extractionMethod());
            invoiceRepository.save(invoice);

            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("[invoiceId={}] EXTRACTION completed method={} textLength={} duration={}ms",
                    invoiceId, result.extractionMethod(), result.rawText().length(), durationMs);
        } catch (Exception e) {
            log.error("[invoiceId={}] EXTRACTION failed reason={}", invoiceId, e.getMessage(), e);
            markFailed(invoiceId, e.getMessage());
        }
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
