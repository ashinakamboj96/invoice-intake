package com.zamp.invoice.extraction;

import com.zamp.invoice.config.ValidationConfig;
import com.zamp.invoice.enums.ExtractionMethod;
import com.zamp.invoice.exception.ExtractionFailedException;
import org.springframework.stereotype.Component;

/**
 * Decides whether an uploaded file has a usable embedded text layer (a digital PDF) or needs OCR
 * (a scanned document). Tries {@link PdfTextExtractor} first — its output is essentially 100%
 * accurate when it's real content — and falls back to OCR only when there isn't enough of it to
 * trust, per {@link ValidationConfig#getMinTextLength()}.
 */
@Component
public class DocumentTypeDetector {

    private final PdfTextExtractor pdfTextExtractor;
    private final ValidationConfig validationConfig;

    public DocumentTypeDetector(PdfTextExtractor pdfTextExtractor, ValidationConfig validationConfig) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.validationConfig = validationConfig;
    }

    /**
     * @param fileBytes the uploaded file, PDF or image
     * @return {@code PDF_TEXT} if PDFBox extracted enough real text to trust; {@code OCR} otherwise
     *         (including when the file isn't a parseable PDF at all, e.g. a JPG/PNG)
     */
    public ExtractionMethod detect(byte[] fileBytes) {
        try {
            String text = pdfTextExtractor.extract(fileBytes);
            int strippedLength = text.replaceAll("\\s+", "").length();
            return strippedLength >= validationConfig.getMinTextLength()
                    ? ExtractionMethod.PDF_TEXT
                    : ExtractionMethod.OCR;
        } catch (ExtractionFailedException e) {
            return ExtractionMethod.OCR;
        }
    }
}
