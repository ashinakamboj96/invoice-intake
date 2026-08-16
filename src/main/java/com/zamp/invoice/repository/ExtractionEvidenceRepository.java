package com.zamp.invoice.repository;

import com.zamp.invoice.model.entity.ExtractionEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** CRUD plus invoice-scoped lookups for OCR evidence records. */
@Repository
public interface ExtractionEvidenceRepository extends JpaRepository<ExtractionEvidence, UUID> {

    List<ExtractionEvidence> findByInvoiceId(UUID invoiceId);

    /** Invoice-level evidence only (line-item rows have {@code lineItemId} set) — backs the detail page's field-confidence summary. */
    List<ExtractionEvidence> findByInvoiceIdAndLineItemIdIsNull(UUID invoiceId);
}
