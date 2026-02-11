package com.snapfit.snapfitbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SnapFitBackEndApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnapFitBackEndApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }

}
