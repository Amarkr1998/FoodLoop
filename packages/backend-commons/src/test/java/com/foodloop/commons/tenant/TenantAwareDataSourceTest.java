package com.foodloop.commons.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Proves the actual mechanism behind ADR-009 against a real Postgres: every
 * connection checkout is stamped with whatever tenant is in
 * {@link TenantContext} at that moment, and a checkout with no tenant set
 * clears the GUC rather than leaking the previous holder's value.
 */
@Testcontainers
class TenantAwareDataSourceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void stampsConnectionWithCurrentTenant() throws Exception {
        TenantAwareDataSource dataSource = new TenantAwareDataSource(rawDataSource());
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);

        try (Connection connection = dataSource.getConnection()) {
            assertThat(currentSetting(connection)).isEqualTo(tenantId.toString());
        }
    }

    @Test
    void differentCheckoutsSeeDifferentTenants() throws Exception {
        TenantAwareDataSource dataSource = new TenantAwareDataSource(rawDataSource());
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        TenantContext.set(tenantA);
        try (Connection connection = dataSource.getConnection()) {
            assertThat(currentSetting(connection)).isEqualTo(tenantA.toString());
        }

        TenantContext.set(tenantB);
        try (Connection connection = dataSource.getConnection()) {
            assertThat(currentSetting(connection)).isEqualTo(tenantB.toString());
        }
    }

    @Test
    void checkoutWithNoTenantSetClearsTheGuc() throws Exception {
        TenantAwareDataSource dataSource = new TenantAwareDataSource(rawDataSource());

        try (Connection connection = dataSource.getConnection()) {
            assertThat(currentSetting(connection)).isEmpty();
        }
    }

    private String currentSetting(Connection connection) throws Exception {
        try (var statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT current_setting('app.current_tenant', true)")) {
            resultSet.next();
            String value = resultSet.getString(1);
            return value == null ? "" : value;
        }
    }

    private PGSimpleDataSource rawDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        return dataSource;
    }
}
