package com.goslogic.orion.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.goslogic.orion.iam")
public class OrionIamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrionIamServiceApplication.class, args);
    }

}
