package com.zamp.invoice.service;

import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.dto.InvoiceDetailResponse;
import com.zamp.invoice.dto.InvoiceListResponse;
import com.zamp.invoice.dto.InvoiceUploadResponse;
import com.zamp.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ExtractionPipelineService extractionPipelineService;

    public InvoiceService(InvoiceRepository invoiceRepository, ExtractionPipelineService extractionPipelineService) {
        this.invoiceRepository = invoiceRepository;
        this.extractionPipelineService = extractionPipelineService;
    }

    public InvoiceUploadResponse uploadInvoice(MultipartFile file) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public InvoiceDetailResponse getInvoice(UUID invoiceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public InvoiceListResponse listInvoices(InvoiceStatus status, int page, int pageSize) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public byte[] downloadOriginalFile(UUID invoiceId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
