package com.zamp.invoice.repository;

import com.zamp.invoice.model.entity.ExtractionEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** CRUD plus invoice-scoped lookups for OCR evidence records. */
@Repository
public interface ExtractionEvidenceRepository extends JpaRepository<ExtractionEvidence, UUID> {

    /** Both invoice-level and line-item-level evidence; the caller partitions by {@code lineItemId} being null. */
    List<ExtractionEvidence> findByInvoiceId(UUID invoiceId);
}
