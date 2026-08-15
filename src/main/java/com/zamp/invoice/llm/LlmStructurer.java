package com.zamp.invoice.llm;

import com.zamp.invoice.extraction.ExtractionResult;
import org.springframework.stereotype.Component;

@Component
public class LlmStructurer {

    private final LlmClient llmClient;

    public LlmStructurer(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public LlmInvoiceResult structure(ExtractionResult extractionResult) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
