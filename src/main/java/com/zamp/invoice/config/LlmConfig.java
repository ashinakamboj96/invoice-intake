package com.zamp.invoice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableConfigurationProperties(LlmProperties.class)
@EnableRetry
public class LlmConfig {
}
