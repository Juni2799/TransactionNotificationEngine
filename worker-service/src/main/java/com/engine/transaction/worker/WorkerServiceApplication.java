package com.engine.transaction.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.engine.transaction.worker", "com.engine.transaction.common"})
@EnableJpaRepositories(basePackages = "com.engine.transaction.common.repository")
@EntityScan(basePackages = "com.engine.transaction.common.model")
public class WorkerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkerServiceApplication.class, args);
    }
}
