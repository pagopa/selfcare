package it.pagopa.selfcare.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Declares how a product's data must be isolated, so consuming microservices can route persistence
 * instead of hardcoding a single database per deployment.
 *
 * <p>Absent configuration means {@link DatabaseIsolationModel#SHARED}, so products that predate this
 * field keep their current behaviour and no data migration is required.
 */
public class DataIsolationConfig {

    private DatabaseIsolationModel database;

    /**
     * Logical name of the dedicated database, meaningful only when {@link #database} is DEDICATED.
     * It is a logical key, never a connection string or any other credential: this configuration is
     * replicated to blob storage and readable by every consumer, which authenticates with its own
     * managed identity.
     */
    private String databaseName;

    public DatabaseIsolationModel getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseIsolationModel database) {
        this.database = database;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /** Never returns null, so callers need not null-check an unconfigured product. */
    public DatabaseIsolationModel resolveDatabaseModel() {
        return database == null ? DatabaseIsolationModel.SHARED : database;
    }

    /**
     * True when this product requires its data to live outside the shared database.
     *
     * <p>Derived helper, kept out of the serialised product JSON: that JSON is the distribution
     * format every consumer reads, so it must carry stored configuration only.
     */
    @JsonIgnore
    public boolean isDedicatedDatabase() {
        return DatabaseIsolationModel.DEDICATED.equals(resolveDatabaseModel());
    }
}
