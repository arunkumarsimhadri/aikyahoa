package com.aikya.platform.multitenancy;

import com.aikya.common.TenantContext;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final Logger log = LoggerFactory.getLogger(SchemaMultiTenantConnectionProvider.class);

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        String schema = (tenantIdentifier == null || tenantIdentifier.isBlank())
                ? TenantContext.DEFAULT_TENANT
                : tenantIdentifier.trim().toLowerCase();

        if (!TenantContext.isValidSchemaName(schema)) {
            throw new SQLException("Invalid tenant schema identifier: " + tenantIdentifier);
        }

        Connection connection = getAnyConnection();
        try {
            applySchema(connection, schema);
            return connection;
        } catch (SQLException ex) {
            releaseAnyConnection(connection);
            throw ex;
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            applySchema(connection, TenantContext.DEFAULT_TENANT);
        } catch (SQLException ex) {
            log.debug("Failed to reset connection schema to public before release: {}", ex.getMessage());
        } finally {
            releaseAnyConnection(connection);
        }
    }

    private void applySchema(Connection connection, String schema) throws SQLException {
        if (!TenantContext.isValidSchemaName(schema)) {
            throw new SQLException("Invalid tenant schema identifier: " + schema);
        }
        String productName = connection.getMetaData().getDatabaseProductName();
        boolean isH2 = productName != null && productName.toLowerCase().contains("h2");

        if (isH2) {
            try (Statement stmt = connection.createStatement()) {
                if (!TenantContext.DEFAULT_TENANT.equalsIgnoreCase(schema)) {
                    stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
                }
                stmt.execute("SET SCHEMA " + schema);
            }
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO " + schema + ", public");
        } catch (SQLException ex) {
            try (Statement fallbackStmt = connection.createStatement()) {
                fallbackStmt.execute("SET SCHEMA " + schema);
            }
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType)
                || SchemaMultiTenantConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new IllegalArgumentException("Cannot unwrap to " + unwrapType.getName());
    }
}
