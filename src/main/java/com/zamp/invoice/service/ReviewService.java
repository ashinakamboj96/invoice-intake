package com.zamp.invoice.service;

import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.model.entity.InvoiceLineItem;
import com.zamp.invoice.enums.InvoiceStatus;
import com.zamp.invoice.enums.ReviewActionType;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.enums.ValidationScope;
import com.zamp.invoice.model.dto.CompleteReviewRequest;
import com.zamp.invoice.model.dto.CompleteReviewResponse;
import com.zamp.invoice.model.dto.ValidationFailureDto;
import com.zamp.invoice.exception.InvalidReviewActionException;
import com.zamp.invoice.exception.InvoiceNotFoundException;
import com.zamp.invoice.repository.InvoiceLineItemRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import com.zamp.invoice.repository.ValidationFailureRepository;
import com.zamp.invoice.validation.ValidationEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Write side of human review: applying a reviewer's resolutions to an invoice's failures and revalidating. See {@link #completeReview} for the core flow. */
@Service
public class ReviewService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final ValidationFailureRepository validationFailureRepository;
    private final ValidationEngine validationEngine;

    public ReviewService(InvoiceRepository invoiceRepository,
                          InvoiceLineItemRepository invoiceLineItemRepository,
                          ValidationFailureRepository validationFailureRepository,
                          ValidationEngine validationEngine) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.validationFailureRepository = validationFailureRepository;
        this.validationEngine = validationEngine;
    }

    /**
     * Applies a human reviewer's resolutions to every currently-unresolved failure on an
     * invoice, then re-validates to see whether it's now clean.
     * <p>
     * Runs as one transaction, including the re-validation step: if resolutions were applied in
     * one transaction and re-validation ran in another, a crash or failure in between would leave
     * failures marked resolved while the invoice's status and failure list were never recomputed
     * to match — an inconsistent state that's worse than the extra time the combined transaction
     * holds its locks for.
     *
     * @param invoiceId the invoice under review; must currently be {@code NEEDS_REVIEW}
     * @param request   one resolution per currently-unresolved failure
     * @return the invoice's resulting status and any failures still outstanding
     * @throws InvoiceNotFoundException     if no invoice exists with this id
     * @throws InvalidReviewActionException if the invoice isn't in {@code NEEDS_REVIEW}, a
     *                                      resolution references an unknown/already-resolved
     *                                      failure, not every unresolved failure is covered, or
     *                                      a {@code CORRECTED} value is missing or invalid
     */
    @Transactional
    public CompleteReviewResponse completeReview(UUID invoiceId, CompleteReviewRequest request) {
        Invoice invoice = fetchInvoiceInReview(invoiceId);
        List<ValidationFailure> unresolvedFailures = validationFailureRepository.findByInvoiceIdAndResolvedFalse(invoiceId);
        List<CompleteReviewRequest.FailureResolution> resolutions = request.getResolutions();
        Map<UUID, ValidationFailure> failuresById = requireAllCovered(unresolvedFailures, resolutions);

        CompleteReviewResponse duplicateConfirmedResponse = handleDuplicateConfirmedIfPresent(invoice, resolutions, failuresById);
        if (duplicateConfirmedResponse != null) {
            return duplicateConfirmedResponse;
        }

        applyResolutions(invoice, unresolvedFailures, failuresById, resolutions);
        invoiceRepository.save(invoice);

        Set<String> skipRules = collectSkipRules(invoiceId, resolutions, failuresById);
        return revalidateAndBuildResponse(invoiceId, skipRules);
    }

    private Invoice fetchInvoiceInReview(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        if (invoice.getStatus() != InvoiceStatus.NEEDS_REVIEW) {
            throw new InvalidReviewActionException("Invoice is not in NEEDS_REVIEW status");
        }
        return invoice;
    }

    /**
     * Verifies every currently-unresolved failure is covered by some resolution in the request,
     * and indexes them by id — one bulk fetch upfront so the rest of this flow looks failures up
     * in memory instead of issuing a {@code findById} per resolution. Skipped entirely when the
     * request confirms a duplicate: that resolution alone rejects the invoice regardless of what
     * else is outstanding (see {@link #handleDuplicateConfirmedIfPresent}), so requiring every
     * other failure to be resolved first would just be busywork with no effect on the outcome.
     */
    private Map<UUID, ValidationFailure> requireAllCovered(List<ValidationFailure> unresolvedFailures,
                                                             List<CompleteReviewRequest.FailureResolution> resolutions) {
        Map<UUID, ValidationFailure> failuresById = unresolvedFailures.stream()
                .collect(Collectors.toMap(ValidationFailure::getId, Function.identity()));

        boolean confirmingDuplicate = resolutions.stream()
                .anyMatch(r -> r.getAction() == ReviewActionType.DUPLICATE_CONFIRMED);

        if (!confirmingDuplicate) {
            Set<UUID> resolutionIds = resolutions.stream()
                    .map(CompleteReviewRequest.FailureResolution::getFailureId)
                    .collect(Collectors.toSet());
            if (!resolutionIds.containsAll(failuresById.keySet())) {
                throw new InvalidReviewActionException("All unresolved failures must be resolved before completing review");
            }
        }
        return failuresById;
    }

    private CompleteReviewResponse handleDuplicateConfirmedIfPresent(Invoice invoice,
                                                                       List<CompleteReviewRequest.FailureResolution> resolutions,
                                                                       Map<UUID, ValidationFailure> failuresById) {
        CompleteReviewRequest.FailureResolution duplicateConfirmed = resolutions.stream()
                .filter(r -> r.getAction() == ReviewActionType.DUPLICATE_CONFIRMED)
                .findFirst()
                .orElse(null);
        if (duplicateConfirmed == null) {
            return null;
        }

        ValidationFailure failure = failuresById.get(duplicateConfirmed.getFailureId());
        if (failure == null) {
            throw new InvalidReviewActionException("Failure not found: " + duplicateConfirmed.getFailureId());
        }
        markResolved(failure, ReviewActionType.DUPLICATE_CONFIRMED, null);
        validationFailureRepository.save(failure);

        invoice.setStatus(InvoiceStatus.REJECTED);
        invoiceRepository.save(invoice);

        return CompleteReviewResponse.builder()
                .status(InvoiceStatus.REJECTED)
                .newFailures(List.of())
                .build();
    }

    /**
     * Applies every resolution, then persists all touched failures and any corrected line items
     * in two bulk {@code saveAll} calls rather than one write per resolution.
     */
    private void applyResolutions(Invoice invoice,
                                   List<ValidationFailure> unresolvedFailures,
                                   Map<UUID, ValidationFailure> failuresById,
                                   List<CompleteReviewRequest.FailureResolution> resolutions) {
        Map<UUID, InvoiceLineItem> lineItemsById = invoiceLineItemRepository.findByInvoiceId(invoice.getId()).stream()
                .collect(Collectors.toMap(InvoiceLineItem::getId, Function.identity()));
        Set<InvoiceLineItem> touchedLineItems = new LinkedHashSet<>();

        for (CompleteReviewRequest.FailureResolution resolution : resolutions) {
            applyResolution(invoice, unresolvedFailures, failuresById, lineItemsById, touchedLineItems, resolution);
        }

        validationFailureRepository.saveAll(unresolvedFailures);
        if (!touchedLineItems.isEmpty()) {
            invoiceLineItemRepository.saveAll(touchedLineItems);
        }
    }

    private void applyResolution(Invoice invoice,
                                  List<ValidationFailure> unresolvedFailures,
                                  Map<UUID, ValidationFailure> failuresById,
                                  Map<UUID, InvoiceLineItem> lineItemsById,
                                  Set<InvoiceLineItem> touchedLineItems,
                                  CompleteReviewRequest.FailureResolution resolution) {
        ValidationFailure failure = failuresById.get(resolution.getFailureId());
        if (failure == null || failure.isResolved()) {
            return;
        }

        if (resolution.getAction() == ReviewActionType.CORRECTED) {
            String newValue = requireNonBlankNewValue(resolution.getNewValue());
            applyCorrection(invoice, failure, newValue, lineItemsById, touchedLineItems);
            markResolved(failure, ReviewActionType.CORRECTED, newValue);
            resolveSiblingFailures(unresolvedFailures, failure);
        } else {
            markResolved(failure, resolution.getAction(), null);
        }
    }

    private String requireNonBlankNewValue(String newValue) {
        if (newValue == null || newValue.isBlank()) {
            throw new InvalidReviewActionException("newValue must not be blank for a CORRECTED resolution");
        }
        return newValue;
    }

    /** A correction on one field also resolves every other unresolved failure on that same field. */
    private void resolveSiblingFailures(List<ValidationFailure> unresolvedFailures, ValidationFailure resolvedFailure) {
        unresolvedFailures.stream()
                .filter(f -> !f.getId().equals(resolvedFailure.getId()))
                .filter(f -> !f.isResolved())
                .filter(f -> f.getScope() == resolvedFailure.getScope())
                .filter(f -> Objects.equals(f.getFieldName(), resolvedFailure.getFieldName()))
                .filter(f -> Objects.equals(f.getLineItemId(), resolvedFailure.getLineItemId()))
                .forEach(sibling -> markResolved(sibling, ReviewActionType.CORRECTED, null));
    }

    private void applyCorrection(Invoice invoice, ValidationFailure failure, String newValue,
                                  Map<UUID, InvoiceLineItem> lineItemsById, Set<InvoiceLineItem> touchedLineItems) {
        FieldName targetField = resolveCorrectionField(failure);
        if (targetField == null) {
            // EXACT_DUPLICATE and anything else with no implied field can only be resolved via
            // APPROVED or DUPLICATE_DISMISSED, not CORRECTED.
            return;
        }
        try {
            if (failure.getScope() == ValidationScope.LINE_ITEM) {
                InvoiceLineItem item = lineItemsById.get(failure.getLineItemId());
                if (item == null) {
                    throw new InvalidReviewActionException("Line item not found: " + failure.getLineItemId());
                }
                applyLineItemCorrection(item, targetField, newValue);
                touchedLineItems.add(item);
            } else {
                applyInvoiceFieldCorrection(invoice, targetField, newValue);
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new InvalidReviewActionException("Invalid value '" + newValue + "' for field " + targetField);
        }
    }

    /**
     * The field a correction applies to. Usually just {@code failure.getFieldName()}, but
     * {@code SUBTOTAL_MISMATCH}, {@code TOTAL_RECONCILIATION}, {@code LINE_TOTAL_MISMATCH}, and
     * {@code MISSING_LINE_ITEM_AMOUNT} are all fieldless (scope {@code INVOICE}/{@code LINE_ITEM}
     * with no single field flagged) even though each unambiguously implicates one editable field —
     * falls back to that implied field so these can be corrected too, not just approved.
     */
    private FieldName resolveCorrectionField(ValidationFailure failure) {
        if (failure.getFieldName() != null) {
            return failure.getFieldName();
        }
        return switch (failure.getRule()) {
            case "SUBTOTAL_MISMATCH" -> FieldName.SUBTOTAL_AMOUNT;
            case "TOTAL_RECONCILIATION" -> FieldName.TOTAL_AMOUNT;
            case "LINE_TOTAL_MISMATCH", "MISSING_LINE_ITEM_AMOUNT" -> FieldName.AMOUNT;
            default -> null;
        };
    }

    private void applyInvoiceFieldCorrection(Invoice invoice, FieldName fieldName, String newValue) {
        switch (fieldName) {
            case VENDOR_NAME -> invoice.setVendorName(newValue);
            case INVOICE_NUMBER -> invoice.setInvoiceNumber(newValue);
            case INVOICE_DATE -> invoice.setInvoiceDate(LocalDate.parse(newValue));
            case CURRENCY -> invoice.setCurrency(newValue);
            case SUBTOTAL_AMOUNT -> invoice.setSubtotalAmount(new BigDecimal(newValue));
            case TAX_AMOUNT -> invoice.setTaxAmount(new BigDecimal(newValue));
            case TOTAL_AMOUNT -> invoice.setTotalAmount(new BigDecimal(newValue));
            default -> {
            }
        }
    }

    private void applyLineItemCorrection(InvoiceLineItem item, FieldName fieldName, String newValue) {
        switch (fieldName) {
            case DESCRIPTION -> item.setDescription(newValue);
            case QUANTITY -> item.setQuantity(new BigDecimal(newValue));
            case UNIT_PRICE -> item.setUnitPrice(new BigDecimal(newValue));
            case AMOUNT -> item.setAmount(new BigDecimal(newValue));
            default -> {
            }
        }
    }

    private void markResolved(ValidationFailure failure, ReviewActionType action, String newValue) {
        failure.setResolved(true);
        failure.setResolvedAt(OffsetDateTime.now());
        failure.setAction(action);
        if (newValue != null) {
            failure.setNewValue(newValue);
        }
    }

    /**
     * Rules to skip on revalidation: those approved in this request, plus any approved in an
     * earlier {@code completeReview} call on this invoice. Without the latter, a rule approved in
     * a previous round has no memory across requests and can re-fire indefinitely whenever a
     * different failure's resolution triggers revalidation.
     */
    private Set<String> collectSkipRules(UUID invoiceId,
                                          List<CompleteReviewRequest.FailureResolution> resolutions,
                                          Map<UUID, ValidationFailure> failuresById) {
        Set<String> approvedThisRound = resolutions.stream()
                .filter(r -> r.getAction() == ReviewActionType.APPROVED)
                .map(r -> failuresById.get(r.getFailureId()))
                .filter(Objects::nonNull)
                .map(ValidationFailure::getRule)
                .collect(Collectors.toSet());

        Set<String> approvedPreviously = validationFailureRepository
                .findByInvoiceIdAndAction(invoiceId, ReviewActionType.APPROVED)
                .stream()
                .map(ValidationFailure::getRule)
                .collect(Collectors.toSet());

        Set<String> skipRules = new HashSet<>(approvedThisRound);
        skipRules.addAll(approvedPreviously);
        return skipRules;
    }

    private CompleteReviewResponse revalidateAndBuildResponse(UUID invoiceId, Set<String> skipRules) {
        validationEngine.validate(invoiceId, skipRules);

        List<ValidationFailure> remainingFailures = validationFailureRepository.findByInvoiceIdAndResolvedFalse(invoiceId);
        Invoice revalidatedInvoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        return CompleteReviewResponse.builder()
                .status(revalidatedInvoice.getStatus())
                .newFailures(remainingFailures.stream().map(ValidationFailureDto::from).toList())
                .build();
    }
}
