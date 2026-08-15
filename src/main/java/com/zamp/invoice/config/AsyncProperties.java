package com.zamp.invoice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "async")
public class AsyncProperties {

    private int corePoolSize = 2;
    private int maxPoolSize = 5;
    private int queueCapacity = 10;
}
