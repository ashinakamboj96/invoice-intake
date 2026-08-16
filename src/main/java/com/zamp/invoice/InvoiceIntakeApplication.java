package com.zamp.invoice;

import com.zamp.invoice.config.DataSourceUrlInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point for Trusted Invoice Intake — see decisions.md for the architecture and reasoning behind the pipeline this app runs. */
@SpringBootApplication
public class InvoiceIntakeApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(InvoiceIntakeApplication.class);
        app.addInitializers(new DataSourceUrlInitializer());
        app.run(args);
    }
}
