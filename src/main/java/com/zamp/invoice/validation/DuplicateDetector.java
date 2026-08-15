package com.zamp.invoice.validation;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DuplicateDetector {

    private final InvoiceRepository invoiceRepository;

    public DuplicateDetector(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<ValidationFailure> detectDuplicates(Invoice invoice) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
