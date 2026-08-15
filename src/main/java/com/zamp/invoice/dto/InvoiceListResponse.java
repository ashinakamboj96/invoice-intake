package com.zamp.invoice.dto;

import java.util.List;

public record InvoiceListResponse(
        List<InvoiceListItem> items,
        long totalCount,
        int page,
        int pageSize
) {
}
