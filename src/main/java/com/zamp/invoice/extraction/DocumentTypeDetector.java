package com.zamp.invoice.extraction;

import com.zamp.invoice.domain.ExtractionMethod;
import org.springframework.stereotype.Component;

@Component
public class DocumentTypeDetector {

    public ExtractionMethod detect(byte[] fileBytes) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
