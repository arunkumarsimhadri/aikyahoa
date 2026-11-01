package com.aikya.platform.multitenancy;

import com.aikya.common.TenantContext;
import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class TenantMigrationService {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrationService.class);

    private final DataSource dataSource;

    public TenantMigrationService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initPublicSchemaOnStartup() {
        migratePublicSchema();
    }

    public void migratePublicSchema() {
        log.info("Running Flyway migrations on public schema...");
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS public");
        } catch (SQLException ex) {
            log.debug("Public schema creation check note: {}", ex.getMessage());
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/public")
                .schemas(TenantContext.DEFAULT_TENANT)
                .defaultSchema(TenantContext.DEFAULT_TENANT)
                .createSchemas(true)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Public schema migrations completed.");
    }

    public void provisionAndMigrateTenantSchema(String schemaName) {
        if (!TenantContext.isValidSchemaName(schemaName)) {
            throw new IllegalArgumentException("Invalid tenant schema identifier: " + schemaName);
        }
        String normalizedSchema = schemaName.trim().toLowerCase();
        log.info("Provisioning and migrating tenant schema: {}", normalizedSchema);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + normalizedSchema);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to create tenant schema " + normalizedSchema, ex);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/tenant")
                .schemas(normalizedSchema)
                .defaultSchema(normalizedSchema)
                .createSchemas(true)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        log.info("Tenant schema {} migrated successfully.", normalizedSchema);
    }
}
