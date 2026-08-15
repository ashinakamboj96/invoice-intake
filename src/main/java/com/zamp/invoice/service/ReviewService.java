package com.zamp.invoice.service;

import com.zamp.invoice.dto.CompleteReviewRequest;
import com.zamp.invoice.dto.CompleteReviewResponse;
import com.zamp.invoice.dto.ValidationFailureDto;
import com.zamp.invoice.repository.InvoiceRepository;
import com.zamp.invoice.repository.ValidationFailureRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private final InvoiceRepository invoiceRepository;
    private final ValidationFailureRepository validationFailureRepository;

    public ReviewService(InvoiceRepository invoiceRepository, ValidationFailureRepository validationFailureRepository) {
        this.invoiceRepository = invoiceRepository;
        this.validationFailureRepository = validationFailureRepository;
    }

    public List<ValidationFailureDto> getPendingFailures(UUID invoiceId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public CompleteReviewResponse completeReview(UUID invoiceId, CompleteReviewRequest request) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
