package com.zamp.invoice.extraction;

import com.zamp.invoice.config.ValidationConfig;
import com.zamp.invoice.domain.ExtractionMethod;
import com.zamp.invoice.exception.ExtractionFailedException;
import org.springframework.stereotype.Component;

@Component
public class DocumentTypeDetector {

    private final PdfTextExtractor pdfTextExtractor;
    private final ValidationConfig validationConfig;

    public DocumentTypeDetector(PdfTextExtractor pdfTextExtractor, ValidationConfig validationConfig) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.validationConfig = validationConfig;
    }

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
