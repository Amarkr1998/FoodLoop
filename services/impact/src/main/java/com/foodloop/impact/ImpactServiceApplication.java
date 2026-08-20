package com.foodloop.impact;

import com.foodloop.commons.tenant.TenantAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TenantAutoConfiguration.class)
public class ImpactServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImpactServiceApplication.class, args);
    }
}
