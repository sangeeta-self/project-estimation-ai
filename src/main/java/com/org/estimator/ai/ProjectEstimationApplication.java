package com.org.estimator.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class})
public class ProjectEstimationApplication {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        SpringApplication.run(ProjectEstimationApplication.class, args);
    }
}
