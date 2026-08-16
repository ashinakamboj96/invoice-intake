package com.zamp.invoice.repository;

import com.zamp.invoice.model.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** CRUD plus invoice-scoped lookups for line items. */
@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {

    List<InvoiceLineItem> findByInvoiceId(UUID invoiceId);
}
