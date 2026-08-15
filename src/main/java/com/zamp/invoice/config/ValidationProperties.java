package com.zamp.invoice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "validation")
public class ValidationProperties {

    private BigDecimal ocrConfidenceThreshold = new BigDecimal("0.90");
    private int minTextLength = 50;
}
