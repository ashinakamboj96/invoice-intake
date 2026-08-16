package com.zamp.invoice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Activates the infrastructure {@code LlmClient}'s {@code @Retryable} method depends on
 * ({@code @EnableRetry}, which turns on Spring Retry's AOP interceptors) and registers
 * {@link LlmProperties}. No beans of its own — the annotations are the whole point.
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
@EnableRetry
public class LlmConfig {
}
