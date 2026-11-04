package com.engine.transaction.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.engine.transaction.ingestion", "com.engine.transaction.common"})
@EnableJpaRepositories(basePackages = "com.engine.transaction.common.repository")
@EntityScan(basePackages = "com.engine.transaction.common.model")
public class IngestionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(IngestionServiceApplication.class, args);
    }
}
