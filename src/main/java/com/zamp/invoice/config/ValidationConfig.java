package com.zamp.invoice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "validation")
public class ValidationConfig {

    private BigDecimal ocrConfidenceThreshold = new BigDecimal("0.90");
    private int minTextLength = 50;
}
