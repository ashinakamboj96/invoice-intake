package com.zamp.invoice.extraction;

import com.zamp.invoice.config.OcrConfig;
import com.zamp.invoice.exception.ExtractionFailedException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OcrExtractorTest {

    private final OcrConfig ocrConfig = new OcrConfig();
    private final OcrExtractor extractor = new OcrExtractor(ocrConfig);

    @Test
    void throwsExtractionFailedOnRandomNonPdfNonImageBytes() {
        byte[] randomBytes = "this is definitely not a pdf or an image".getBytes(StandardCharsets.UTF_8);

        assertThrows(ExtractionFailedException.class, () -> extractor.extract(randomBytes, "invoice.jpg"));
    }

    @Test
    void throwsExtractionFailedOnEmptyByteArray() {
        assertThrows(ExtractionFailedException.class, () -> extractor.extract(new byte[0], "invoice.pdf"));
    }

    /**
     * Manual/integration verification only — requires a real Tesseract install with tessdata
     * at the path configured via `ocr.tessdata-path` (see application.yml). Not run in CI.
     * <p>
     * To verify manually: set {@code SAMPLE_IMAGE_PATH} below to a real image of printed text,
     * un-skip this test, and run:
     * {@code mvn test -Dtest=OcrExtractorTest#realOcrReadsSampleImage}
     */
    @Disabled("Requires a real Tesseract installation and tessdata; run manually to verify OCR")
    @Test
    void realOcrReadsSampleImage() throws Exception {
        String sampleImagePath = "/path/to/sample-invoice.png";
        ocrConfig.setTessDataPath("/opt/homebrew/share/tessdata");

        byte[] imageBytes = Files.readAllBytes(Path.of(sampleImagePath));

        ExtractionResult result = extractor.extract(imageBytes, "sample-invoice.png");

        System.out.println("OCR raw text:\n" + result.getRawText());
        System.out.println("Word count: " + result.getWords().size());
        assertThat(result.getRawText()).isNotBlank();
    }
}
