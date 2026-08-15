package com.zamp.invoice.llm;

import com.zamp.invoice.config.LlmProperties;
import org.springframework.stereotype.Component;

@Component
public class LlmClient {

    private final LlmProperties properties;

    public LlmClient(LlmProperties properties) {
        this.properties = properties;
    }

    public String complete(String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
