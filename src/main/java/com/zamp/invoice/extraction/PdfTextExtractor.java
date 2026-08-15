package com.zamp.invoice.extraction;

import com.zamp.invoice.exception.ExtractionFailedException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfTextExtractor {

    public String extract(byte[] fileBytes) throws ExtractionFailedException {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder result = new StringBuilder();
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                if (page > 1) {
                    result.append("\n--- PAGE ").append(page).append(" ---\n");
                }
                result.append(stripper.getText(document));
            }
            return result.toString();
        } catch (IOException | RuntimeException e) {
            throw new ExtractionFailedException("Failed to extract text from PDF", e);
        }
    }
}
