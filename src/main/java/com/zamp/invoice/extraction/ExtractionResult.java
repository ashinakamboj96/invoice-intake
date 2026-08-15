package com.zamp.invoice.extraction;

import com.zamp.invoice.domain.ExtractionMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResult {

    private String rawText;
    private List<OcrWord> words;
    private ExtractionMethod extractionMethod;
}
