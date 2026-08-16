package com.zamp.invoice.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamp.invoice.exception.LlmUnavailableException;
import com.zamp.invoice.model.llm.LlmInvoiceResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmStructurerTest {

    private final LlmClient llmClient = mock(LlmClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LlmStructurer structurer = new LlmStructurer(llmClient, objectMapper);

    @Test
    void parsesValidJsonIntoLlmInvoiceResult() {
        String json = """
                {
                  "vendorName": "Acme Supplies Inc.",
                  "invoiceNumber": "INV-20481",
                  "invoiceDate": "2026-03-14",
                  "currency": "USD",
                  "subtotalAmount": 275.00,
                  "taxAmount": 22.00,
                  "totalAmount": 297.00,
                  "lineItems": [
                    {
                      "lineNumber": 1,
                      "description": "Widget A",
                      "quantity": 10,
                      "unitPrice": 12.50,
                      "amount": 125.00
                    }
                  ]
                }
                """;
        when(llmClient.complete(any(), any())).thenReturn(json);

        LlmInvoiceResult result = structurer.structure("raw invoice text");

        assertThat(result.getVendorName()).isEqualTo("Acme Supplies Inc.");
        assertThat(result.getInvoiceNumber()).isEqualTo("INV-20481");
        assertThat(result.getInvoiceDate()).isEqualTo("2026-03-14");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("275.00"));
        assertThat(result.getTaxAmount()).isEqualByComparingTo(new BigDecimal("22.00"));
        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("297.00"));
        assertThat(result.getLineItems()).hasSize(1);
        assertThat(result.getLineItems().get(0).getDescription()).isEqualTo("Widget A");
        assertThat(result.getLineItems().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("125.00"));
    }

    @Test
    void parsesJsonWithNullFieldsAndEmptyLineItems() {
        String json = """
                {
                  "vendorName": null,
                  "invoiceNumber": null,
                  "invoiceDate": null,
                  "currency": null,
                  "subtotalAmount": null,
                  "taxAmount": null,
                  "totalAmount": null,
                  "lineItems": []
                }
                """;
        when(llmClient.complete(any(), any())).thenReturn(json);

        LlmInvoiceResult result = structurer.structure("raw invoice text");

        assertThat(result.getVendorName()).isNull();
        assertThat(result.getTotalAmount()).isNull();
        assertThat(result.getLineItems()).isEmpty();
    }

    @Test
    void throwsLlmUnavailableExceptionOnMalformedJson() {
        when(llmClient.complete(any(), any())).thenReturn("this is not json");

        assertThrows(LlmUnavailableException.class, () -> structurer.structure("raw invoice text"));
    }
}
