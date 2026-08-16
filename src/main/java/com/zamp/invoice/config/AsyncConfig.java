package com.zamp.invoice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Enables {@code @Async} and provides the dedicated thread pool the extraction pipeline runs on,
 * plus a catch-all handler for exceptions that escape an {@code @Async} method — which Spring
 * would otherwise only log, silently leaving the invoice stuck in {@code PROCESSING} forever.
 */
@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(AsyncProperties.class)
public class AsyncConfig implements AsyncConfigurer {

    private final AsyncProperties properties;

    public AsyncConfig(AsyncProperties properties) {
        this.properties = properties;
    }

    /** Backs every {@code @Async} method in the extraction pipeline; sized via {@link AsyncProperties}. */
    @Bean(name = "extractionTaskExecutor")
    public Executor extractionTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getCorePoolSize());
        executor.setMaxPoolSize(properties.getMaxPoolSize());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("invoice-pipeline-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return this::handleUncaughtException;
    }

    private void handleUncaughtException(Throwable throwable, Method method, Object... params) {
        Object invoiceId = params.length > 0 ? params[0] : "unknown";
        log.error("[invoiceId={}] Uncaught exception escaped async method={}", invoiceId, method.getName(), throwable);
    }
}
