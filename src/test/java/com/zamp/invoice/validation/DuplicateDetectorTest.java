package com.zamp.invoice.validation;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceStatus;
import com.zamp.invoice.domain.ValidationFailure;
import com.zamp.invoice.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DuplicateDetectorTest {

    private final InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
    private final DuplicateDetector detector = new DuplicateDetector(invoiceRepository);

    private Invoice newInvoice() {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .vendorName("Acme Supplies Inc.")
                .invoiceNumber("INV-20481")
                .build();
    }

    @Test
    void noExistingInvoiceProducesNoFailure() {
        Invoice invoice = newInvoice();
        when(invoiceRepository.findPotentialExactDuplicates(any(), any(), any(), any())).thenReturn(List.of());

        List<ValidationFailure> failures = detector.detect(invoice);

        assertThat(failures).isEmpty();
    }

    @Test
    void existingInvoiceWithSameVendorAndNumberProducesExactDuplicate() {
        Invoice invoice = newInvoice();
        Invoice existing = Invoice.builder()
                .id(UUID.randomUUID())
                .vendorName("Acme Supplies Inc.")
                .invoiceNumber("INV-20481")
                .status(InvoiceStatus.ACCEPTED)
                .build();
        when(invoiceRepository.findPotentialExactDuplicates(any(), any(), any(), any())).thenReturn(List.of(existing));

        List<ValidationFailure> failures = detector.detect(invoice);

        assertThat(failures).hasSize(1);
        assertThat(failures.get(0).getRule()).isEqualTo("EXACT_DUPLICATE");
        assertThat(failures.get(0).getRelatedInvoice()).isEqualTo(existing);
    }

    @Test
    void failedStatusInvoicesAreRequestedAsExcluded() {
        Invoice invoice = newInvoice();
        when(invoiceRepository.findPotentialExactDuplicates(any(), any(), any(), any())).thenReturn(List.of());

        detector.detect(invoice);

        verify(invoiceRepository).findPotentialExactDuplicates(any(), any(), any(),
                argThat(statuses -> statuses.contains(InvoiceStatus.FAILED)));
    }

    @Test
    void selfExclusionPassesOwnIdAsExcludeId() {
        Invoice invoice = newInvoice();
        when(invoiceRepository.findPotentialExactDuplicates(any(), any(), any(), any())).thenReturn(List.of());

        detector.detect(invoice);

        verify(invoiceRepository).findPotentialExactDuplicates(eq(invoice.getId()), any(), any(), any());
    }
}
