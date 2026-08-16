package com.zamp.invoice.enums;

/** How an invoice's raw text was obtained from the uploaded file. */
public enum ExtractionMethod {
    /** Text came straight from the PDF's embedded text layer (PDFBox); no OCR involved. */
    PDF_TEXT,
    /** Text was recognized from a rendered page image via Tesseract OCR. */
    OCR
}
