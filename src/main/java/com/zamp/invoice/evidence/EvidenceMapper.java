package com.zamp.invoice.evidence;

import com.zamp.invoice.domain.ExtractionEvidence;
import com.zamp.invoice.domain.Invoice;
import com.zamp.invoice.extraction.ExtractionResult;
import com.zamp.invoice.llm.LlmInvoiceResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EvidenceMapper {

    public List<ExtractionEvidence> mapEvidence(Invoice invoice, LlmInvoiceResult llmResult, ExtractionResult extractionResult) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
