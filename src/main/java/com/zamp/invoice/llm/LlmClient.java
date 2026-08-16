package com.zamp.invoice.llm;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import com.zamp.invoice.config.LlmProperties;
import com.zamp.invoice.exception.LlmUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Thin wrapper around the OpenAI chat completion API — the only class in the codebase that
 * actually calls out to the LLM provider. Deliberately has no invoice-domain knowledge; it just
 * sends a system/user message pair and returns the raw response text, retrying once via Spring
 * Retry before giving up.
 */
@Slf4j
@Component
public class LlmClient {

    private final LlmProperties properties;
    private final OpenAiService openAiService;

    public LlmClient(LlmProperties properties) {
        this.properties = properties;
        this.openAiService = new OpenAiService(properties.getApiKey(), Duration.ofSeconds(properties.getTimeoutSeconds()));
    }

    /**
     * @return the raw text of the model's reply (expected to be JSON, but not parsed here)
     * @throws LlmUnavailableException via {@link #recover} once both attempts have failed
     */
    @Retryable(retryFor = RuntimeException.class, maxAttempts = 2, listeners = "llmRetryListener")
    public String complete(String systemPrompt, String userMessage) throws LlmUnavailableException {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(properties.getModel())
                .messages(List.of(new ChatMessage("system", systemPrompt), new ChatMessage("user", userMessage)))
                .build();

        ChatCompletionResult result = openAiService.createChatCompletion(request);
        return result.getChoices().get(0).getMessage().getContent();
    }

    /** Invoked by Spring Retry once {@link #complete} has exhausted its attempts; converts the raw failure into our domain exception. */
    @Recover
    public String recover(RuntimeException e, String systemPrompt, String userMessage) {
        throw new LlmUnavailableException("OpenAI call failed after retries", e);
    }
}
