package com.zamp.invoice.model.extraction;

import com.zamp.invoice.enums.ExtractionMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Output of the extraction stage, handed to {@code LlmStructurer}: the raw text to structure,
 * plus which method produced it. {@code words} carries per-word OCR results with bounding boxes
 * for {@code EvidenceMapper} to match back against — always empty for {@code PDF_TEXT}
 * extractions, since there's no OCR confidence to record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {

    private String rawText;
    private List<OcrWord> words;
    private ExtractionMethod extractionMethod;
}
