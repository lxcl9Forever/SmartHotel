package com.smarthotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmarthotelBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                SmarthotelBackendApplication.class,
                args
        );
    }
}