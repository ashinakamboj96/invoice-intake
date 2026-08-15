package com.zamp.invoice.validation;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.domain.ValidationScope;
import com.zamp.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class DuplicateDetector {

    private static final List<InvoiceStatus> EXCLUDED_STATUSES = List.of(InvoiceStatus.FAILED, InvoiceStatus.PROCESSING);

    private final InvoiceRepository invoiceRepository;

    public DuplicateDetector(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public List<ValidationFailure> detect(Invoice invoice) {
        if (invoice.getVendorName() == null || invoice.getInvoiceNumber() == null) {
            return List.of();
        }

        List<Invoice> candidates = invoiceRepository.findPotentialExactDuplicates(invoice.getId(), invoice.getVendorName(), invoice.getInvoiceNumber().trim(), EXCLUDED_STATUSES);

        String normalizedVendor = normalize(invoice.getVendorName());
        return candidates.stream()
                .filter(candidate -> normalize(candidate.getVendorName()).equals(normalizedVendor))
                .findFirst()
                .map(candidate -> buildFailure(invoice, candidate))
                .map(List::of)
                .orElseGet(List::of);
    }

    private ValidationFailure buildFailure(Invoice invoice, Invoice candidate) {
        String message = "Invoice " + invoice.getInvoiceNumber() + " from " + invoice.getVendorName()
                + " already exists in the system.";
        return ValidationFailure.builder()
                .id(UUID.randomUUID())
                .invoice(invoice)
                .scope(ValidationScope.INVOICE)
                .rule("EXACT_DUPLICATE")
                .relatedInvoice(candidate)
                .message(message)
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
