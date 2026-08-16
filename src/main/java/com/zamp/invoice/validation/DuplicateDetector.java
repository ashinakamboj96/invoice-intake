package com.zamp.invoice.validation;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.domain.ReviewActionType;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.domain.ValidationScope;
import com.zamp.invoice.repository.InvoiceRepository;
import com.zamp.invoice.repository.ValidationFailureRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class DuplicateDetector {

    private static final List<InvoiceStatus> EXCLUDED_STATUSES = List.of(InvoiceStatus.FAILED, InvoiceStatus.PROCESSING);

    private final InvoiceRepository invoiceRepository;
    private final ValidationFailureRepository validationFailureRepository;

    public DuplicateDetector(InvoiceRepository invoiceRepository, ValidationFailureRepository validationFailureRepository) {
        this.invoiceRepository = invoiceRepository;
        this.validationFailureRepository = validationFailureRepository;
    }

    /**
     * Looks for another non-{@code FAILED}, non-{@code PROCESSING} invoice with the same
     * (normalized) vendor name and exact invoice number, and flags the first match found. A
     * candidate a human has already dismissed as not-a-duplicate for this specific invoice pair
     * (a {@code DUPLICATE_DISMISSED} review action) is skipped rather than re-flagged.
     *
     * @param invoice the invoice to check for duplicates; must have a vendor name and invoice
     *                number, otherwise there's nothing to match on
     * @return a single-element list with the {@code EXACT_DUPLICATE} failure if a match was
     *         found; empty otherwise
     */
    public List<ValidationFailure> detect(Invoice invoice) {
        if (invoice.getVendorName() == null || invoice.getInvoiceNumber() == null) {
            return List.of();
        }

        List<Invoice> candidates = invoiceRepository.findPotentialExactDuplicates(invoice.getId(), invoice.getVendorName(), invoice.getInvoiceNumber().trim(), EXCLUDED_STATUSES);

        String normalizedVendor = normalize(invoice.getVendorName());
        return candidates.stream()
                .filter(candidate -> normalize(candidate.getVendorName()).equals(normalizedVendor))
                .filter(candidate -> !isAlreadyDismissed(invoice, candidate))
                .findFirst()
                .map(candidate -> buildFailure(invoice, candidate))
                .map(List::of)
                .orElseGet(List::of);
    }

    private boolean isAlreadyDismissed(Invoice invoice, Invoice candidate) {
        return validationFailureRepository.existsByInvoiceIdAndRelatedInvoiceIdAndAction(
                invoice.getId(), candidate.getId(), ReviewActionType.DUPLICATE_DISMISSED);
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
