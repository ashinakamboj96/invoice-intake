package com.zamp.invoice.service;

import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.enums.InvoiceStatus;
import com.zamp.invoice.model.dto.InvoiceListResponse;
import com.zamp.invoice.repository.ExtractionEvidenceRepository;
import com.zamp.invoice.repository.InvoiceLineItemRepository;
import com.zamp.invoice.repository.InvoiceRepository;
import com.zamp.invoice.repository.ValidationFailureRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceListTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceLineItemRepository invoiceLineItemRepository;
    @Mock
    private ValidationFailureRepository validationFailureRepository;
    @Mock
    private ExtractionEvidenceRepository extractionEvidenceRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private Invoice sampleInvoice() {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .vendorName("Acme Supplies Inc.")
                .invoiceNumber("INV-1")
                .status(InvoiceStatus.ACCEPTED)
                .uploadedAt(OffsetDateTime.now())
                .build();
    }

    @SuppressWarnings("unchecked")
    private void stubFindAll(Page<Invoice> page) {
        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
    }

    @SuppressWarnings("unchecked")
    private Specification<Invoice> captureSpecification() {
        ArgumentCaptor<Specification<Invoice>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(invoiceRepository).findAll(captor.capture(), any(Pageable.class));
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private Pageable capturePageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(invoiceRepository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }

    @Test
    void noFiltersReturnsAllInvoicesSortedByUploadedAtDescending() {
        Invoice invoice = sampleInvoice();
        stubFindAll(new PageImpl<>(List.of(invoice), Pageable.ofSize(20), 1));

        InvoiceListResponse response = invoiceService.listInvoices(null, null, null, null, null, null, 0, 20);

        assertThat(response.getInvoices()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);

        Pageable pageable = capturePageable();
        assertThat(pageable.getSort().getOrderFor("uploadedAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("uploadedAt").isDescending()).isTrue();
    }

    @Test
    void statusFilterSpecificationIncludesStatusPredicate() {
        stubFindAll(new PageImpl<>(List.of()));

        invoiceService.listInvoices(InvoiceStatus.NEEDS_REVIEW, null, null, null, null, null, 0, 20);

        Specification<Invoice> spec = captureSpecification();
        Root<Invoice> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<Object> statusPath = mock(Path.class);
        when(root.get("status")).thenReturn(statusPath);

        spec.toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(cb).equal(statusPath, InvoiceStatus.NEEDS_REVIEW);
    }

    @Test
    void vendorFilterIsCaseInsensitiveContains() {
        stubFindAll(new PageImpl<>(List.of()));

        invoiceService.listInvoices(null, "Acme", null, null, null, null, 0, 20);

        Specification<Invoice> spec = captureSpecification();
        Root<Invoice> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<String> vendorPath = mock(Path.class);
        Expression<String> loweredVendor = mock(Expression.class);
        when(root.<String>get("vendorName")).thenReturn(vendorPath);
        when(cb.lower(vendorPath)).thenReturn(loweredVendor);

        spec.toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(cb).like(loweredVendor, "%acme%");
    }

    @Test
    void dateRangeFilterAppliesFromAndTo() {
        stubFindAll(new PageImpl<>(List.of()));
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        invoiceService.listInvoices(null, null, from, to, null, null, 0, 20);

        Specification<Invoice> spec = captureSpecification();
        Root<Invoice> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<LocalDate> datePath = mock(Path.class);
        when(root.<LocalDate>get("invoiceDate")).thenReturn(datePath);

        spec.toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(cb).greaterThanOrEqualTo(datePath, from);
        verify(cb).lessThanOrEqualTo(datePath, to);
    }

    @Test
    void amountRangeFilterAppliesMinAndMax() {
        stubFindAll(new PageImpl<>(List.of()));
        BigDecimal min = new BigDecimal("100.00");
        BigDecimal max = new BigDecimal("500.00");

        invoiceService.listInvoices(null, null, null, null, min, max, 0, 20);

        Specification<Invoice> spec = captureSpecification();
        Root<Invoice> root = mock(Root.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Path<BigDecimal> amountPath = mock(Path.class);
        when(root.<BigDecimal>get("totalAmount")).thenReturn(amountPath);

        spec.toPredicate(root, mock(CriteriaQuery.class), cb);

        verify(cb).greaterThanOrEqualTo(amountPath, min);
        verify(cb).lessThanOrEqualTo(amountPath, max);
    }

    @Test
    void unresolvedFailureCountIsPopulatedPerInvoice() {
        Invoice invoice = sampleInvoice();
        stubFindAll(new PageImpl<>(List.of(invoice), Pageable.ofSize(20), 1));
        when(validationFailureRepository.countByInvoiceIdAndResolvedFalse(invoice.getId())).thenReturn(3);

        InvoiceListResponse response = invoiceService.listInvoices(null, null, null, null, null, null, 0, 20);

        assertThat(response.getInvoices()).hasSize(1);
        assertThat(response.getInvoices().get(0).getUnresolvedFailureCount()).isEqualTo(3);
    }
}
