package com.zamp.invoice.repository;

import com.zamp.invoice.domain.ReviewActionType;
import com.zamp.invoice.domain.ValidationFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ValidationFailureRepository extends JpaRepository<ValidationFailure, UUID> {

    List<ValidationFailure> findByInvoiceId(UUID invoiceId);

    List<ValidationFailure> findByInvoiceIdAndResolvedFalse(UUID invoiceId);

    boolean existsByInvoiceIdAndRelatedInvoiceIdAndAction(UUID invoiceId, UUID relatedInvoiceId, ReviewActionType action);

    int countByInvoiceIdAndResolvedFalse(UUID invoiceId);
}
