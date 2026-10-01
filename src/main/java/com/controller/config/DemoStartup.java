package com.controller.config;

import com.controller.service.DemoCompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DemoStartup implements CommandLineRunner {

    private final DemoCompanyService demoCompanyService;

    @Override
    public void run(String... args) {
        try {
            demoCompanyService.getOrCreateDemoCompany();
        } catch (Exception e) {
            log.warn("Could not seed demo company on startup: {}", e.getMessage());
        }
    }
}
