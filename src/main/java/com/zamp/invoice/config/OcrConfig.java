package com.zamp.invoice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@code ocr.*} in application.yml — where Tesseract's trained-data files live on this host. */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ocr")
public class OcrConfig {

    private String tessDataPath;
}
