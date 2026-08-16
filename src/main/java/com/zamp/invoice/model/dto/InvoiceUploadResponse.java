package com.zamp.invoice.model.dto;

import com.zamp.invoice.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Immediate response for {@code POST /invoices}: acknowledges the upload while extraction runs asynchronously. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceUploadResponse {

    private UUID id;
    private InvoiceStatus status;
    private String originalFilename;
    private OffsetDateTime uploadedAt;
}
