package com.zamp.invoice.dto;

import com.zamp.invoice.domain.InvoiceStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceUploadResponse(
        UUID id,
        InvoiceStatus status,
        String originalFilename,
        OffsetDateTime uploadedAt
) {
}
