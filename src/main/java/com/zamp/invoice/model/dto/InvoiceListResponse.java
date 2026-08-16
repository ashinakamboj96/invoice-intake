package com.zamp.invoice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Paginated response for {@code GET /api/invoices}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceListResponse {

    private List<InvoiceListItem> invoices;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
