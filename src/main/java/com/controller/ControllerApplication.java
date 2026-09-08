package com.controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Password encoding and CORS are configured once, in SecurityConfig,
 * to avoid duplicate bean definitions and conflicting CORS setups.
 */
@SpringBootApplication
public class ControllerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ControllerApplication.class, args);
    }
}