package com.plandi;

import org.springframework.boot.SpringApplication;

public class TestPlandiApplication {

    public static void main(String[] args) {
        SpringApplication.from(PlandiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
