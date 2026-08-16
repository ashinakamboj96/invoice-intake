package com.zamp.invoice.extraction;

import com.zamp.invoice.config.OcrConfig;
import com.zamp.invoice.enums.ExtractionMethod;
import com.zamp.invoice.exception.ExtractionFailedException;
import com.zamp.invoice.model.extraction.ExtractionResult;
import com.zamp.invoice.model.extraction.OcrWord;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Renders each page of a scanned PDF or image to a bitmap and runs Tesseract over it, capturing
 * both the recognized text and per-word confidence/position ({@link OcrWord}) for later evidence
 * mapping.
 */
@Component
public class OcrExtractor {

    private static final int RENDER_DPI = 300;
    private static final int MIN_EXTRACTED_CHARACTERS = 10;

    private final OcrConfig ocrConfig;

    public OcrExtractor(OcrConfig ocrConfig) {
        this.ocrConfig = ocrConfig;
    }

    /**
     * @param fileBytes the file to OCR — a PDF (rendered page by page) or a plain image
     * @param filename  used only to decide whether to treat {@code fileBytes} as a PDF
     * @return the recognized text plus every word's confidence and bounding box
     * @throws ExtractionFailedException if OCR recognized fewer than {@value #MIN_EXTRACTED_CHARACTERS}
     *                                    non-whitespace characters, or the file couldn't be rendered at all
     */
    public ExtractionResult extract(byte[] fileBytes, String filename) throws ExtractionFailedException {
        try {
            List<BufferedImage> pages = renderPages(fileBytes, filename);
            ITesseract tesseract = createTesseract();

            List<OcrWord> allWords = new ArrayList<>();
            StringBuilder rawText = new StringBuilder();
            int pageNumber = 0;
            for (BufferedImage page : pages) {
                pageNumber++;
                if (pageNumber > 1) {
                    rawText.append("\n--- PAGE ").append(pageNumber).append(" ---\n");
                }
                List<Word> words = tesseract.getWords(page, ITessAPI.TessPageIteratorLevel.RIL_WORD);
                boolean firstWordOnPage = true;
                for (Word word : words) {
                    // Tess4J returns confidence on a 0–100 scale; normalize to 0–1 for storage and threshold checks
                    allWords.add(new OcrWord(word.getText(), word.getConfidence() / 100f, word.getBoundingBox()));
                    if (!firstWordOnPage) {
                        rawText.append(' ');
                    }
                    rawText.append(word.getText());
                    firstWordOnPage = false;
                }
            }

            String text = rawText.toString();
            if (text.replaceAll("\\s+", "").length() < MIN_EXTRACTED_CHARACTERS) {
                throw new ExtractionFailedException("Document could not be read by OCR");
            }

            return new ExtractionResult(text, allWords, ExtractionMethod.OCR);
        } catch (ExtractionFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtractionFailedException("Failed to extract text via OCR", e);
        }
    }

    // ITesseract is not thread-safe, so a fresh instance is created per call rather than reused as
    // a Spring singleton bean — concurrent extractions (the pipeline runs on a multi-thread pool)
    // would otherwise corrupt each other's state.
    private ITesseract createTesseract() {
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath(ocrConfig.getTessDataPath());
        tesseract.setLanguage("eng");
        tesseract.setVariable("user_defined_dpi", String.valueOf(RENDER_DPI));
        return tesseract;
    }

    private List<BufferedImage> renderPages(byte[] fileBytes, String filename) throws IOException {
        if (isPdf(filename)) {
            return renderPdfPages(fileBytes);
        }
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(fileBytes));
        if (image != null) {
            return List.of(image);
        }
        return renderPdfPages(fileBytes);
    }

    private boolean isPdf(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    private List<BufferedImage> renderPdfPages(byte[] fileBytes) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                images.add(renderer.renderImageWithDPI(i, RENDER_DPI));
            }
        }
        return images;
    }
}
