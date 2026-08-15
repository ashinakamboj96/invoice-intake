CREATE TYPE invoice_status AS ENUM (
    'PROCESSING', 'ACCEPTED', 'NEEDS_REVIEW', 'FAILED', 'REJECTED'
);
CREATE TYPE extraction_method AS ENUM ('PDF_TEXT', 'OCR');
CREATE TYPE field_name AS ENUM (
    'VENDOR_NAME', 'INVOICE_NUMBER', 'INVOICE_DATE', 'CURRENCY',
    'SUBTOTAL_AMOUNT', 'TAX_AMOUNT', 'TOTAL_AMOUNT',
    'DESCRIPTION', 'QUANTITY', 'UNIT_PRICE', 'AMOUNT'
);
CREATE TYPE validation_scope AS ENUM ('INVOICE_FIELD', 'LINE_ITEM', 'INVOICE');
CREATE TYPE review_action_type AS ENUM (
    'APPROVED', 'CORRECTED', 'DUPLICATE_CONFIRMED', 'DUPLICATE_DISMISSED'
);

CREATE TABLE invoice (
    id                  UUID PRIMARY KEY,
    original_file       BYTEA NOT NULL,
    original_filename   TEXT NOT NULL,
    uploaded_at         TIMESTAMPTZ NOT NULL,
    status              invoice_status NOT NULL DEFAULT 'PROCESSING',
    extraction_method   extraction_method,
    vendor_name         TEXT,
    invoice_number      TEXT,
    invoice_date        DATE,
    currency            TEXT,
    subtotal_amount     NUMERIC,
    tax_amount          NUMERIC,
    total_amount        NUMERIC,
    failure_message     TEXT
);

CREATE TABLE invoice_line_item (
    id              UUID PRIMARY KEY,
    invoice_id      UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    line_number     INTEGER NOT NULL,
    description     TEXT,
    quantity        NUMERIC,
    unit_price      NUMERIC,
    amount          NUMERIC,
    CONSTRAINT uq_invoice_line_number UNIQUE (invoice_id, line_number),
    CONSTRAINT uq_line_item_invoice UNIQUE (invoice_id, id)
);

CREATE TABLE extraction_evidence (
    id              UUID PRIMARY KEY,
    invoice_id      UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    line_item_id    UUID,
    field_name      field_name NOT NULL,
    ocr_confidence  NUMERIC,
    CONSTRAINT fk_evidence_line_item
        FOREIGN KEY (invoice_id, line_item_id)
        REFERENCES invoice_line_item(invoice_id, id)
        ON DELETE CASCADE,
    CONSTRAINT chk_ocr_confidence
        CHECK (ocr_confidence IS NULL OR (ocr_confidence >= 0 AND ocr_confidence <= 1)),
    CONSTRAINT uq_evidence_field
        UNIQUE NULLS NOT DISTINCT (invoice_id, line_item_id, field_name)
);

CREATE TABLE validation_failure (
    id                  UUID PRIMARY KEY,
    invoice_id          UUID NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    scope               validation_scope NOT NULL,
    line_item_id        UUID,
    field_name          field_name,
    rule                TEXT NOT NULL,
    related_invoice_id  UUID REFERENCES invoice(id) ON DELETE SET NULL,
    message             TEXT NOT NULL,
    resolved            BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at         TIMESTAMPTZ,
    action              review_action_type,
    new_value           TEXT,
    CONSTRAINT fk_failure_line_item
        FOREIGN KEY (invoice_id, line_item_id)
        REFERENCES invoice_line_item(invoice_id, id)
        ON DELETE CASCADE,
    CONSTRAINT chk_not_self_duplicate
        CHECK (related_invoice_id != invoice_id),
    CONSTRAINT chk_scope_consistency CHECK (
        (scope = 'INVOICE_FIELD' AND field_name IS NOT NULL AND line_item_id IS NULL)
        OR (scope = 'LINE_ITEM' AND line_item_id IS NOT NULL)
        OR (scope = 'INVOICE' AND field_name IS NULL AND line_item_id IS NULL)
    ),
    CONSTRAINT chk_resolved_consistency CHECK (
        (resolved = FALSE AND resolved_at IS NULL AND action IS NULL)
        OR (resolved = TRUE AND resolved_at IS NOT NULL AND action IS NOT NULL)
    )
);

CREATE INDEX idx_invoice_status ON invoice(status);
CREATE INDEX idx_invoice_vendor ON invoice(vendor_name);
CREATE INDEX idx_invoice_date ON invoice(invoice_date);
CREATE INDEX idx_invoice_amount ON invoice(total_amount);
CREATE INDEX idx_validation_failure_invoice_resolved ON validation_failure(invoice_id, resolved);
