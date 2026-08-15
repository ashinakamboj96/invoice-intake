package com.zamp.invoice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import java.util.UUID;

/**
 * {@code line_item_id} is left as a plain UUID rather than a JPA association: the DB enforces it via a
 * composite foreign key on (invoice_id, line_item_id) -&gt; invoice_line_item(invoice_id, id), which JPA
 * cannot model cleanly as an object reference.
 */
@Entity
@Table(name = "extraction_evidence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "invoice")
public class ExtractionEvidence {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "line_item_id")
    private UUID lineItemId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "field_name", nullable = false, columnDefinition = "field_name")
    private FieldName fieldName;

    @Column(name = "ocr_confidence")
    private BigDecimal ocrConfidence;
}
