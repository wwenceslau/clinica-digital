package com.clinicadigital.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.clinicadigital")
@EntityScan("com.clinicadigital")
public class ClinicGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicGatewayApplication.class, args);
    }
}
