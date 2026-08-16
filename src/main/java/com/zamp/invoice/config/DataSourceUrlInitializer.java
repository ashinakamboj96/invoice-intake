package com.zamp.invoice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Render's {@code fromDatabase} / {@code connectionString} injects the datasource URL as
 * {@code postgres://user:password@host:port/db}, but the PostgreSQL JDBC driver requires
 * {@code jdbc:postgresql://host:port/db} and, unlike libpq-style URIs, does not accept
 * {@code user[:password]@} in the authority component — it's parsed as part of the hostname,
 * producing an {@code UnknownHostException}. Credentials are supplied separately via
 * {@code spring.datasource.username}/{@code password} (also sourced from {@code fromDatabase}
 * in render.yaml), so the userinfo is stripped, not just the scheme. Rewritten at startup,
 * before the DataSource bean reads the property, rather than requiring Render-specific URL
 * handling in application.yml.
 */
public class DataSourceUrlInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(DataSourceUrlInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment env = context.getEnvironment();
        String url = env.getProperty("spring.datasource.url", "");

        if (url.startsWith("postgres://")) {
            String jdbcUrl = url.replaceFirst("^postgres://(?:[^@/]*@)?", "jdbc:postgresql://");
            env.getPropertySources().addFirst(
                    new MapPropertySource("renderUrlFix", Map.of("spring.datasource.url", jdbcUrl))
            );
            log.info("DataSourceUrlInitializer: rewrote postgres:// URL to jdbc:postgresql://");
        }
    }
}
