package com.foodloop.commons.tenant;

import com.foodloop.commons.web.CorrelationIdFilter;
import com.foodloop.commons.web.GlobalExceptionHandler;
import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires this platform's cross-cutting request infrastructure into a service
 * with a single {@code @Import(TenantAutoConfiguration.class)} (deliberately
 * explicit rather than {@code spring.factories} auto-configuration magic —
 * this is security-critical wiring, worth seeing at the import site of every
 * service that needs it): row-level-security tenant enforcement (wraps the
 * primary {@link DataSource} bean so every connection checkout is stamped
 * with the caller's tenant, {@link TenantAwareDataSource}), correlation-ID
 * propagation into logs, and the platform-wide error envelope
 * ({@link GlobalExceptionHandler}) — none of these live under a service's
 * own base package, so Spring Boot's default component scan never finds
 * them without this explicit import. Pair with registering
 * {@link TenantFilter} and {@link CorrelationIdFilter} in the service's
 * Spring Security filter chain.
 */
@Configuration
public class TenantAutoConfiguration {

    @Bean
    public static BeanPostProcessor tenantDataSourceBeanPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(dataSource);
                }
                return bean;
            }
        };
    }

    @Bean
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
