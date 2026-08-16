package com.zamp.invoice.repository;

import com.zamp.invoice.enums.ReviewActionType;
import com.zamp.invoice.model.entity.ValidationFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** CRUD plus the queries {@code ValidationEngine} and {@code ReviewService} need. */
@Repository
public interface ValidationFailureRepository extends JpaRepository<ValidationFailure, UUID> {

    List<ValidationFailure> findByInvoiceId(UUID invoiceId);

    List<ValidationFailure> findByInvoiceIdAndResolvedFalse(UUID invoiceId);

    /** Used to avoid re-flagging a duplicate pair the reviewer already dismissed for this invoice. */
    boolean existsByInvoiceIdAndRelatedInvoiceIdAndAction(UUID invoiceId, UUID relatedInvoiceId, ReviewActionType action);

    int countByInvoiceIdAndResolvedFalse(UUID invoiceId);

    /** Rules approved in any past review round for this invoice — the cross-round half of the revalidation skip-set. */
    List<ValidationFailure> findByInvoiceIdAndAction(UUID invoiceId, ReviewActionType action);
}
