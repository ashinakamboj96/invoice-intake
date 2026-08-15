package com.zamp.invoice.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamp.invoice.exception.LlmUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LlmStructurer {

    private static final String SYSTEM_PROMPT = """
            You are an invoice data extraction assistant.
            You will be given raw text extracted from an invoice document.
            Your job is to identify and extract structured invoice data.

            Rules you MUST follow:
            - Return ONLY valid JSON. No explanation, no markdown, no code blocks.
            - If you cannot find a field in the text, return null for that field.
            - Do NOT calculate or infer missing values. If subtotal is not stated, return null.
            - Do NOT invent values. Only extract what is explicitly present in the text.
            - invoiceDate must be in ISO 8601 format (YYYY-MM-DD) or null.
            - All numeric values must be numbers (not strings). Do not include currency symbols.
            - lineItems must be an array. If no line items found, return an empty array.
            - lineNumber must start at 1 and be sequential.
            - currency should be the ISO 4217 code (e.g. INR, USD). Infer from symbols if needed.

            Return this exact JSON structure:
            {
              "vendorName": string or null,
              "invoiceNumber": string or null,
              "invoiceDate": "YYYY-MM-DD" or null,
              "currency": string or null,
              "subtotalAmount": number or null,
              "taxAmount": number or null,
              "totalAmount": number or null,
              "lineItems": [
                {
                  "lineNumber": number,
                  "description": string or null,
                  "quantity": number or null,
                  "unitPrice": number or null,
                  "amount": number or null
                }
              ]
            }
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public LlmStructurer(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public LlmInvoiceResult structure(String rawText) throws LlmUnavailableException {
        String response = llmClient.complete(SYSTEM_PROMPT, rawText);
        try {
            return objectMapper.readValue(response, LlmInvoiceResult.class);
        } catch (Exception e) {
            log.error("[LLM] returned malformed JSON: {}", response);
            throw new LlmUnavailableException("LLM returned malformed JSON", e);
        }
    }
}
