package com.zamp.invoice.repository;

import com.zamp.invoice.domain.ExtractionEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExtractionEvidenceRepository extends JpaRepository<ExtractionEvidence, UUID> {

    List<ExtractionEvidence> findByInvoiceId(UUID invoiceId);

    List<ExtractionEvidence> findByInvoiceIdAndLineItemIdIsNull(UUID invoiceId);
}
