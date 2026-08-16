package com.zamp.invoice.service;

import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.enums.InvoiceStatus;
import com.zamp.invoice.enums.ReviewActionType;
import com.zamp.invoice.model.entity.ValidationFailure;
import com.zamp.invoice.enums.ValidationScope;
import com.zamp.invoice.model.dto.CompleteReviewRequest;
import com.zamp.invoice.model.dto.CompleteReviewResponse;
import com.zamp.invoice.exception.InvalidReviewActionException;
import com.zamp.invoice.repository.InvoiceLineItemRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import com.zamp.invoice.repository.ValidationFailureRepository;
import com.zamp.invoice.validation.ValidationEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock
    private ValidationFailureRepository validationFailureRepository;
    @Mock
    private ValidationEngine validationEngine;

    @InjectMocks
    private ReviewService reviewService;

    private CompleteReviewRequest requestWith(CompleteReviewRequest.FailureResolution... resolutions) {
        return new CompleteReviewRequest(List.of(resolutions));
    }

    @Test
    void allApprovedWithNoNewFailuresProducesAccepted() {
        UUID invoiceId = UUID.randomUUID();
        UUID failureId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(invoiceId).status(InvoiceStatus.NEEDS_REVIEW).build();
        ValidationFailure failure = ValidationFailure.builder()
                .id(failureId).scope(ValidationScope.INVOICE).rule("SUBTOTAL_MISMATCH")
                .message("mismatch").resolved(false).build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(validationFailureRepository.findByInvoiceIdAndResolvedFalse(invoiceId))
                .thenReturn(List.of(failure))
                .thenReturn(List.of());
        doAnswer(inv -> {
            invoice.setStatus(InvoiceStatus.ACCEPTED);
            return null;
        }).when(validationEngine).validate(eq(invoiceId), any());

        CompleteReviewRequest request = requestWith(
                new CompleteReviewRequest.FailureResolution(failureId, ReviewActionType.APPROVED, null));

        CompleteReviewResponse response = reviewService.completeReview(invoiceId, request);

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.ACCEPTED);
        assertThat(response.getNewFailures()).isEmpty();
    }

    @Test
    void duplicateConfirmedRejectsImmediatelyWithoutRunningValidation() {
        UUID invoiceId = UUID.randomUUID();
        UUID failureId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(invoiceId).status(InvoiceStatus.NEEDS_REVIEW).build();
        ValidationFailure failure = ValidationFailure.builder()
                .id(failureId).scope(ValidationScope.INVOICE).rule("EXACT_DUPLICATE")
                .message("duplicate").resolved(false).build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(validationFailureRepository.findByInvoiceIdAndResolvedFalse(invoiceId)).thenReturn(List.of(failure));

        CompleteReviewRequest request = requestWith(
                new CompleteReviewRequest.FailureResolution(failureId, ReviewActionType.DUPLICATE_CONFIRMED, null));

        CompleteReviewResponse response = reviewService.completeReview(invoiceId, request);

        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.REJECTED);
        assertThat(response.getNewFailures()).isEmpty();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REJECTED);
        assertThat(failure.isResolved()).isTrue();
        assertThat(failure.getAction()).isEqualTo(ReviewActionType.DUPLICATE_CONFIRMED);
        verifyNoInteractions(validationEngine);
    }

    @Test
    void correctionAppliedAndRevalidationFindsNewFailure() {
        UUID invoiceId = UUID.randomUUID();
        UUID failureId = UUID.randomUUID();
        Invoice invoice = Invoice.builder()
                .id(invoiceId).status(InvoiceStatus.NEEDS_REVIEW)
                .subtotalAmount(new BigDecimal("100.00"))
                .taxAmount(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("110.00"))
                .build();
        ValidationFailure subtotalFailure = ValidationFailure.builder()
                .id(failureId).scope(ValidationScope.INVOICE_FIELD).fieldName(FieldName.SUBTOTAL_AMOUNT)
                .rule("SUBTOTAL_MISMATCH").message("mismatch").resolved(false).build();
        ValidationFailure newTotalReconciliationFailure = ValidationFailure.builder()
                .id(UUID.randomUUID()).scope(ValidationScope.INVOICE).rule("TOTAL_RECONCILIATION")
                .message("still mismatched").resolved(false).build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(validationFailureRepository.findByInvoiceIdAndResolvedFalse(invoiceId))
                .thenReturn(List.of(subtotalFailure))
                .thenReturn(List.of(newTotalReconciliationFailure));
        doAnswer(inv -> {
            invoice.setStatus(InvoiceStatus.NEEDS_REVIEW);
            return null;
        }).when(validationEngine).validate(eq(invoiceId), any());

        CompleteReviewRequest request = requestWith(
                new CompleteReviewRequest.FailureResolution(failureId, ReviewActionType.CORRECTED, "200.00"));

        CompleteReviewResponse response = reviewService.completeReview(invoiceId, request);

        assertThat(invoice.getSubtotalAmount()).isEqualByComparingTo("200.00");
        assertThat(response.getStatus()).isEqualTo(InvoiceStatus.NEEDS_REVIEW);
        assertThat(response.getNewFailures()).anyMatch(f -> f.getRule().equals("TOTAL_RECONCILIATION"));
    }

    @Test
    void invoiceNotInNeedsReviewThrows() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(invoiceId).status(InvoiceStatus.ACCEPTED).build();
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        CompleteReviewRequest request = requestWith(
                new CompleteReviewRequest.FailureResolution(UUID.randomUUID(), ReviewActionType.APPROVED, null));

        assertThrows(InvalidReviewActionException.class, () -> reviewService.completeReview(invoiceId, request));
    }

    @Test
    void missingFailureInResolutionsThrows() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(invoiceId).status(InvoiceStatus.NEEDS_REVIEW).build();
        ValidationFailure failure1 = ValidationFailure.builder()
                .id(UUID.randomUUID()).scope(ValidationScope.INVOICE).rule("SUBTOTAL_MISMATCH").resolved(false).build();
        ValidationFailure failure2 = ValidationFailure.builder()
                .id(UUID.randomUUID()).scope(ValidationScope.INVOICE).rule("TOTAL_RECONCILIATION").resolved(false).build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(validationFailureRepository.findByInvoiceIdAndResolvedFalse(invoiceId))
                .thenReturn(List.of(failure1, failure2));

        CompleteReviewRequest request = requestWith(
                new CompleteReviewRequest.FailureResolution(failure1.getId(), ReviewActionType.APPROVED, null));

        assertThrows(InvalidReviewActionException.class, () -> reviewService.completeReview(invoiceId, request));
    }

    @Test
    void approvedRuleIsSkippedDuringRevalidation() {
        UUID invoiceId = UUID.randomUUID();
        UUID failureId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(invoiceId).status(InvoiceStatus.NEEDS_REVIEW).build();
        ValidationFailure failure = ValidationFailure.builder()
                .id(failureId).scope(ValidationScope.INVOICE).rule("SUBTOTAL_MISMATCH")
                .message("mismatch").resolved(false).build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(validationFailureRepository.findByInvoiceIdAndResolvedFalse(invoiceId))
                .thenReturn(List.of(failure))
                .thenReturn(List.of());
        doAnswer(inv -> {
            invoice.setStatus(InvoiceStatus.ACCEPTED);
            return null;
        }).when(validationEngine).validate(eq(invoiceId), any());

        CompleteReviewRequest request = requestWith(
                new CompleteReviewRequest.FailureResolution(failureId, ReviewActionType.APPROVED, null));

        CompleteReviewResponse response = reviewService.completeReview(invoiceId, request);

        assertThat(response.getNewFailures()).noneMatch(f -> f.getRule().equals("SUBTOTAL_MISMATCH"));
        verify(validationEngine).validate(eq(invoiceId), argThat(set -> set.contains("SUBTOTAL_MISMATCH")));
    }

    @Test
    void previouslyApprovedRuleIsNotResurrectedByALaterRoundsRevalidation() {
        UUID invoiceId = UUID.randomUUID();
        UUID lowConfidenceFailureId = UUID.randomUUID();
        UUID ocrSourceFailureId = UUID.randomUUID();
        Invoice invoice = Invoice.builder().id(invoiceId).status(InvoiceStatus.NEEDS_REVIEW).build();

        ValidationFailure lowConfidenceFailure = ValidationFailure.builder()
                .id(lowConfidenceFailureId).scope(ValidationScope.INVOICE_FIELD).fieldName(FieldName.TOTAL_AMOUNT)
                .rule("LOW_OCR_CONFIDENCE").message("low confidence").resolved(false).build();
        ValidationFailure ocrSourceFailure = ValidationFailure.builder()
                .id(ocrSourceFailureId).scope(ValidationScope.LINE_ITEM).lineItemId(UUID.randomUUID())
                .fieldName(FieldName.DESCRIPTION).rule("OCR_SOURCE_NOT_FOUND").message("source not found").resolved(false).build();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(validationFailureRepository.findByInvoiceIdAndResolvedFalse(invoiceId))
                .thenReturn(List.of(lowConfidenceFailure))
                .thenReturn(List.of(ocrSourceFailure))
                .thenReturn(List.of(ocrSourceFailure))
                .thenReturn(List.of());
        doAnswer(inv -> {
            invoice.setStatus(InvoiceStatus.NEEDS_REVIEW);
            return null;
        }).doAnswer(inv -> {
            invoice.setStatus(InvoiceStatus.ACCEPTED);
            return null;
        }).when(validationEngine).validate(eq(invoiceId), any());

        // Round 1: approve LOW_OCR_CONFIDENCE; OCR_SOURCE_NOT_FOUND re-fires, nothing approved previously.
        when(validationFailureRepository.findByInvoiceIdAndAction(invoiceId, ReviewActionType.APPROVED))
                .thenReturn(List.of());
        CompleteReviewRequest round1 = requestWith(
                new CompleteReviewRequest.FailureResolution(lowConfidenceFailureId, ReviewActionType.APPROVED, null));
        CompleteReviewResponse round1Response = reviewService.completeReview(invoiceId, round1);

        assertThat(round1Response.getStatus()).isEqualTo(InvoiceStatus.NEEDS_REVIEW);
        assertThat(round1Response.getNewFailures()).anyMatch(f -> f.getRule().equals("OCR_SOURCE_NOT_FOUND"));
        verify(validationEngine).validate(eq(invoiceId), argThat(set ->
                set.contains("LOW_OCR_CONFIDENCE") && !set.contains("OCR_SOURCE_NOT_FOUND")));

        // Round 2: approve OCR_SOURCE_NOT_FOUND; LOW_OCR_CONFIDENCE was approved last round and must stay skipped.
        when(validationFailureRepository.findByInvoiceIdAndAction(invoiceId, ReviewActionType.APPROVED))
                .thenReturn(List.of(lowConfidenceFailure));
        CompleteReviewRequest round2 = requestWith(
                new CompleteReviewRequest.FailureResolution(ocrSourceFailureId, ReviewActionType.APPROVED, null));
        CompleteReviewResponse round2Response = reviewService.completeReview(invoiceId, round2);

        assertThat(round2Response.getStatus()).isEqualTo(InvoiceStatus.ACCEPTED);
        assertThat(round2Response.getNewFailures()).isEmpty();
        verify(validationEngine).validate(eq(invoiceId), argThat(set ->
                set.contains("LOW_OCR_CONFIDENCE") && set.contains("OCR_SOURCE_NOT_FOUND")));
    }
}
