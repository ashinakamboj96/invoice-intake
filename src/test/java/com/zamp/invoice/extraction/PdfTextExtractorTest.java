package com.zamp.invoice.extraction;

import com.zamp.invoice.exception.ExtractionFailedException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @Test
    void extractsTextFromRealPdf() throws IOException {
        byte[] pdfBytes = createSimplePdf("Hello Invoice World");

        String text = extractor.extract(pdfBytes);

        assertThat(text).isNotEmpty();
        assertThat(text).contains("Hello Invoice World");
    }

    @Test
    void throwsExtractionFailedOnEmptyByteArray() {
        assertThrows(ExtractionFailedException.class, () -> extractor.extract(new byte[0]));
    }

    @Test
    void throwsExtractionFailedOnNonPdfBytes() {
        byte[] randomBytes = "this is definitely not a pdf file".getBytes(StandardCharsets.UTF_8);

        assertThrows(ExtractionFailedException.class, () -> extractor.extract(randomBytes));
    }

    private byte[] createSimplePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}
