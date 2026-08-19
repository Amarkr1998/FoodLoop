package com.foodloop.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://example.invalid/jwks",
            "spring.cloud.gateway.routes[0].id=noop",
            "spring.cloud.gateway.routes[0].uri=http://localhost",
            "spring.cloud.gateway.routes[0].predicates[0]=Path=/__noop__"
        })
class ApiGatewayApplicationTests {

    @Test
    void contextLoads(ApplicationContext context) {
        assertThat(context).isNotNull();
    }
}
