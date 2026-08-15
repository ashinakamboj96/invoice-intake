package com.zamp.invoice.dto;

import com.zamp.invoice.domain.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

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
