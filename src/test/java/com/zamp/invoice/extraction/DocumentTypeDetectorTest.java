package com.zamp.invoice.extraction;

import com.zamp.invoice.config.ValidationConfig;
import com.zamp.invoice.enums.ExtractionMethod;
import com.zamp.invoice.exception.ExtractionFailedException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentTypeDetectorTest {

    private final PdfTextExtractor pdfTextExtractor = mock(PdfTextExtractor.class);
    private final ValidationConfig validationConfig = new ValidationConfig();
    private final DocumentTypeDetector detector = new DocumentTypeDetector(pdfTextExtractor, validationConfig);

    @Test
    void returnsPdfTextWhenExtractedTextMeetsThreshold() {
        when(pdfTextExtractor.extract(any())).thenReturn("x".repeat(60));

        ExtractionMethod result = detector.detect(new byte[]{1, 2, 3});

        assertThat(result).isEqualTo(ExtractionMethod.PDF_TEXT);
    }

    @Test
    void returnsOcrWhenExtractedTextBelowThreshold() {
        when(pdfTextExtractor.extract(any())).thenReturn("too short");

        ExtractionMethod result = detector.detect(new byte[]{1, 2, 3});

        assertThat(result).isEqualTo(ExtractionMethod.OCR);
    }

    @Test
    void returnsOcrWhenFileIsNotAPdf() {
        when(pdfTextExtractor.extract(any())).thenThrow(new ExtractionFailedException("not a pdf"));

        ExtractionMethod result = detector.detect(new byte[]{1, 2, 3});

        assertThat(result).isEqualTo(ExtractionMethod.OCR);
    }
}
