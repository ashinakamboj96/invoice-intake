package com.zamp.invoice.repository;

import com.zamp.invoice.model.entity.Invoice;
import com.zamp.invoice.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * CRUD plus the queries the search API and duplicate detection need. Extends
 * {@link JpaSpecificationExecutor} so {@code InvoiceSpecification}'s dynamic filters
 * (status/vendor/date/amount) can compose without one query method per filter combination.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    /** Candidates for {@code DuplicateDetector}: same invoice number, case-insensitive vendor match done here, exact-normalization match done by the caller. */
    @Query("SELECT i FROM Invoice i WHERE i.id != :excludeId AND LOWER(i.vendorName) = LOWER(:vendorName) AND i.invoiceNumber = :invoiceNumber AND i.status NOT IN :excludeStatuses")
    List<Invoice> findPotentialExactDuplicates(
            @Param("excludeId") UUID excludeId,
            @Param("vendorName") String vendorName,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("excludeStatuses") List<InvoiceStatus> excludeStatuses
    );
}
