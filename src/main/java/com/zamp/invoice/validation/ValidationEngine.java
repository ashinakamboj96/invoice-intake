package com.zamp.invoice.validation;

import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceLineItem;
import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.repository.ExtractionEvidenceRepository;
import com.zamp.invoice.repository.InvoiceLineItemRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import com.zamp.invoice.repository.ValidationFailureRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Component
public class ValidationEngine {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final ExtractionEvidenceRepository extractionEvidenceRepository;
    private final ValidationFailureRepository validationFailureRepository;
    private final FieldValidator fieldValidator;
    private final OcrConfidenceValidator ocrConfidenceValidator;
    private final LineItemValidator lineItemValidator;
    private final InvoiceValidator invoiceValidator;
    private final DuplicateDetector duplicateDetector;

    public ValidationEngine(InvoiceRepository invoiceRepository,
                             InvoiceLineItemRepository invoiceLineItemRepository,
                             ExtractionEvidenceRepository extractionEvidenceRepository,
                             ValidationFailureRepository validationFailureRepository,
                             FieldValidator fieldValidator,
                             OcrConfidenceValidator ocrConfidenceValidator,
                             LineItemValidator lineItemValidator,
                             InvoiceValidator invoiceValidator,
                             DuplicateDetector duplicateDetector) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.extractionEvidenceRepository = extractionEvidenceRepository;
        this.validationFailureRepository = validationFailureRepository;
        this.fieldValidator = fieldValidator;
        this.ocrConfidenceValidator = ocrConfidenceValidator;
        this.lineItemValidator = lineItemValidator;
        this.invoiceValidator = invoiceValidator;
        this.duplicateDetector = duplicateDetector;
    }

    /**
     * Runs all validators against the invoice, persists any failures found, and sets the
     * invoice's status to {@code ACCEPTED} or {@code NEEDS_REVIEW} accordingly (unless it's
     * already {@code FAILED}, which is left untouched). Equivalent to
     * {@link #validate(UUID, Set)} with no excluded rules.
     *
     * @param invoiceId the invoice to validate
     */
    public void validate(UUID invoiceId) {
        validate(invoiceId, Set.of());
    }

    /**
     * Same as {@link #validate(UUID)}, but drops any failure whose {@code rule} is in
     * {@code excludedRules} before persisting. Used when re-validating after a human has just
     * approved specific rules during review, so those approved-but-not-fixed issues don't
     * immediately reappear as new failures.
     *
     * @param invoiceId     the invoice to validate
     * @param excludedRules rule names to exclude from the persisted results
     */
    public void validate(UUID invoiceId, Set<String> excludedRules) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        List<InvoiceLineItem> lineItems = invoiceLineItemRepository.findByInvoiceId(invoiceId);
        List<ExtractionEvidence> evidence = extractionEvidenceRepository.findByInvoiceId(invoiceId);

        List<ValidationFailure> failures = new ArrayList<>();
        failures.addAll(runValidator("FieldValidator", () -> fieldValidator.validate(invoice, evidence)));
        failures.addAll(runValidator("OcrConfidenceValidator", () -> ocrConfidenceValidator.validate(invoice, evidence, lineItems)));
        failures.addAll(runValidator("LineItemValidator", () -> lineItemValidator.validate(invoice, lineItems)));
        failures.addAll(runValidator("InvoiceValidator", () -> invoiceValidator.validate(invoice, lineItems)));
        failures.addAll(runValidator("DuplicateDetector", () -> duplicateDetector.detect(invoice)));

        List<ValidationFailure> filtered = failures.stream()
                .filter(failure -> !excludedRules.contains(failure.getRule()))
                .toList();

        validationFailureRepository.saveAll(filtered);

        if (invoice.getStatus() != InvoiceStatus.FAILED) {
            invoice.setStatus(filtered.isEmpty() ? InvoiceStatus.ACCEPTED : InvoiceStatus.NEEDS_REVIEW);
            invoiceRepository.save(invoice);
        }
    }

    private List<ValidationFailure> runValidator(String name, Supplier<List<ValidationFailure>> validator) {
        try {
            List<ValidationFailure> result = validator.get();
            return result != null ? result : List.of();
        } catch (Exception e) {
            log.error("Validator {} failed: {}", name, e.getMessage(), e);
            return List.of();
        }
    }
}
