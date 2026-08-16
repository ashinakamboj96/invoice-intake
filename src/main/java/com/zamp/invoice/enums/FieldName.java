package com.zamp.invoice.enums;

/**
 * Every field the extraction pipeline can populate, spanning both invoice-level fields and
 * line-item fields. Used to key {@link ValidationScope#INVOICE_FIELD}/{@code LINE_ITEM} failures
 * and OCR evidence records back to the specific field they concern.
 */
public enum FieldName {
    VENDOR_NAME,
    INVOICE_NUMBER,
    INVOICE_DATE,
    CURRENCY,
    SUBTOTAL_AMOUNT,
    TAX_AMOUNT,
    TOTAL_AMOUNT,
    DESCRIPTION,
    QUANTITY,
    UNIT_PRICE,
    AMOUNT
}
