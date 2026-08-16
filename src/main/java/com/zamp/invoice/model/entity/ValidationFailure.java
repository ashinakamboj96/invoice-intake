package com.zamp.invoice.model.entity;

import com.zamp.invoice.enums.FieldName;
import com.zamp.invoice.enums.ReviewActionType;
import com.zamp.invoice.enums.ValidationScope;
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

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One deterministic validation rule failing for one invoice — the record a human reviewer works
 * through on the review screen. Doubles as the review-state record: {@code resolved}/{@code
 * action}/{@code newValue} are set in place once a reviewer acts, rather than in a separate audit
 * table (see decisions.md for why). {@code action == APPROVED} also makes {@code rule} permanently
 * skipped on future revalidation passes for this invoice.
 * <p>
 * {@code line_item_id} is left as a plain UUID rather than a JPA association: the DB enforces it via a
 * composite foreign key on (invoice_id, line_item_id) -&gt; invoice_line_item(invoice_id, id), which JPA
 * cannot model cleanly as an object reference.
 */
@Entity
@Table(name = "validation_failure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"invoice", "relatedInvoice"})
public class ValidationFailure {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "scope", nullable = false, columnDefinition = "validation_scope")
    private ValidationScope scope;

    @Column(name = "line_item_id")
    private UUID lineItemId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "field_name", columnDefinition = "field_name")
    private FieldName fieldName;

    @Column(name = "rule", nullable = false)
    private String rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_invoice_id")
    private Invoice relatedInvoice;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "action", columnDefinition = "review_action_type")
    private ReviewActionType action;

    @Column(name = "new_value")
    private String newValue;
}
