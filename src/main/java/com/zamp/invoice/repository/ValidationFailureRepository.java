package com.zamp.invoice.repository;

import com.zamp.invoice.enums.ReviewActionType;
import com.zamp.invoice.model.entity.ValidationFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Bulk form of {@link #countByInvoiceIdAndResolvedFalse}, for building a list page without one count query per row. */
    @Query("SELECT vf.invoice.id AS invoiceId, COUNT(vf) AS failureCount FROM ValidationFailure vf " +
            "WHERE vf.invoice.id IN :invoiceIds AND vf.resolved = false GROUP BY vf.invoice.id")
    List<UnresolvedCount> countUnresolvedByInvoiceIdIn(@Param("invoiceIds") List<UUID> invoiceIds);

    interface UnresolvedCount {
        UUID getInvoiceId();
        long getFailureCount();
    }

    /** Rules approved in any past review round for this invoice — the cross-round half of the revalidation skip-set. */
    List<ValidationFailure> findByInvoiceIdAndAction(UUID invoiceId, ReviewActionType action);
}
