package com.zamp.invoice.service;

import com.zamp.invoice.evidence.EvidenceMapper;
import com.zamp.invoice.extraction.DocumentTypeDetector;
import com.zamp.invoice.extraction.OcrExtractor;
import com.zamp.invoice.extraction.PdfTextExtractor;
import com.zamp.invoice.llm.LlmStructurer;
import com.zamp.invoice.repository.InvoiceRepository;
import com.zamp.invoice.validation.ValidationEngine;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ExtractionPipelineService {

    private final InvoiceRepository invoiceRepository;
    private final DocumentTypeDetector documentTypeDetector;
    private final PdfTextExtractor pdfTextExtractor;
    private final OcrExtractor ocrExtractor;
    private final LlmStructurer llmStructurer;
    private final EvidenceMapper evidenceMapper;
    private final ValidationEngine validationEngine;

    public ExtractionPipelineService(InvoiceRepository invoiceRepository,
                                      DocumentTypeDetector documentTypeDetector,
                                      PdfTextExtractor pdfTextExtractor,
                                      OcrExtractor ocrExtractor,
                                      LlmStructurer llmStructurer,
                                      EvidenceMapper evidenceMapper,
                                      ValidationEngine validationEngine) {
        this.invoiceRepository = invoiceRepository;
        this.documentTypeDetector = documentTypeDetector;
        this.pdfTextExtractor = pdfTextExtractor;
        this.ocrExtractor = ocrExtractor;
        this.llmStructurer = llmStructurer;
        this.evidenceMapper = evidenceMapper;
        this.validationEngine = validationEngine;
    }

    @Async("extractionTaskExecutor")
    public CompletableFuture<Void> process(UUID invoiceId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
