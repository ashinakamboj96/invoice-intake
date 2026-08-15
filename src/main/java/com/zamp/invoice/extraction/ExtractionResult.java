package com.zamp.invoice.extraction;

import com.zamp.invoice.domain.ExtractionMethod;

import java.util.List;

public record ExtractionResult(
        String rawText,
        List<OcrWord> words,
        ExtractionMethod extractionMethod
) {
}
