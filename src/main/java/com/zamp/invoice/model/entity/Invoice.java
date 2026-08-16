package com.zamp.invoice.model.entity;

import com.zamp.invoice.enums.ExtractionMethod;
import com.zamp.invoice.enums.InvoiceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The central record: one uploaded invoice, its extracted fields, and its review status. The
 * original file bytes live here too ({@link #originalFile}) rather than in an object store — see
 * decisions.md for why. {@code vendorName} through {@code totalAmount} start null and are filled
 * in by {@code LlmStructurer} once extraction completes; they stay null if the pipeline fails
 * before structuring runs.
 */
@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"originalFile", "lineItems"})
public class Invoice {

    @Id
    private UUID id;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "original_file", nullable = false)
    private byte[] originalFile;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "invoice_status")
    private InvoiceStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "extraction_method", columnDefinition = "extraction_method")
    private ExtractionMethod extractionMethod;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "currency")
    private String currency;

    @Column(name = "subtotal_amount")
    private BigDecimal subtotalAmount;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "failure_message")
    private String failureMessage;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();
}
