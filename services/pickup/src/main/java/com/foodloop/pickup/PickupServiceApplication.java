package com.foodloop.pickup;

import com.foodloop.commons.tenant.TenantAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TenantAutoConfiguration.class)
public class PickupServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PickupServiceApplication.class, args);
    }
}
