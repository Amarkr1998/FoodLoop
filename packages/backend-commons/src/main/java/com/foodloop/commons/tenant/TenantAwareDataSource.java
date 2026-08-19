package com.foodloop.commons.tenant;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Stamps every JDBC connection checked out of the pool with the current
 * request's tenant, via Postgres's {@code set_config}, before any query on
 * that connection runs. This is what makes the row-level-security policies
 * on every table (ADR-009) actually see {@code app.current_tenant} —
 * without it, RLS policies keyed on that GUC would see an empty value and
 * (correctly, since the policies are deny-by-default) return no rows at all.
 *
 * <p>Uses session-level {@code SET} (via {@code set_config(..., false)})
 * rather than {@code SET LOCAL}, because at the point a connection is
 * checked out the transaction may not yet be started. Correctness does not
 * depend on resetting the value on return to the pool: every checkout sets
 * it fresh for the checking-out thread's tenant before that connection is
 * used, so a connection previously used by tenant A can never be read by
 * tenant B without first having A's value overwritten by B's.
 *
 * <p>Uses a bind parameter (not string concatenation) even though the value
 * is a UUID, so this is not a SQL-injection vector regardless of what feeds
 * {@link TenantContext}.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    private static final String SET_TENANT_SQL = "SELECT set_config('app.current_tenant', ?, false)";

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applyTenant(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applyTenant(connection);
        return connection;
    }

    private void applyTenant(Connection connection) throws SQLException {
        UUID tenantId = TenantContext.get();
        try (PreparedStatement statement = connection.prepareStatement(SET_TENANT_SQL)) {
            statement.setString(1, tenantId != null ? tenantId.toString() : "");
            statement.execute();
        }
    }
}
