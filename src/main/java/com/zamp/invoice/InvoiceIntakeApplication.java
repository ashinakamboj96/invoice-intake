package com.zamp.invoice;

import com.zamp.invoice.config.DataSourceUrlInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InvoiceIntakeApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(InvoiceIntakeApplication.class);
        app.addInitializers(new DataSourceUrlInitializer());
        app.run(args);
    }
}
