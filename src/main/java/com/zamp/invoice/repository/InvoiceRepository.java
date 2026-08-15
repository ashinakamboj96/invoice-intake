package com.zamp.invoice.repository;

import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    @Query("SELECT i FROM Invoice i WHERE i.id != :excludeId AND LOWER(i.vendorName) = LOWER(:vendorName) AND i.invoiceNumber = :invoiceNumber AND i.status NOT IN :excludeStatuses")
    List<Invoice> findPotentialExactDuplicates(
            @Param("excludeId") UUID excludeId,
            @Param("vendorName") String vendorName,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("excludeStatuses") List<InvoiceStatus> excludeStatuses
    );
}
