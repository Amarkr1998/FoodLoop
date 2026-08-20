package com.foodloop.ngo;

import com.foodloop.commons.tenant.TenantAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TenantAutoConfiguration.class)
public class NgoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NgoServiceApplication.class, args);
    }
}
