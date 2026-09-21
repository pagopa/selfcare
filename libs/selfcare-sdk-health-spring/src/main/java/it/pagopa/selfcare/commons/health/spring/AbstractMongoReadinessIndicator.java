package it.pagopa.selfcare.commons.health.spring;

import com.mongodb.ConnectionString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractMongoReadinessIndicator extends AbstractReadinessIndicator {

    public static final String HOST_NOT_AVAILABLE = "n/a";

    protected abstract String databaseName();

    protected String host() {
        return "";
    }

    @Override
    protected Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>(3);
        data.put(HealthIndicatorConstants.DETAIL_COMPONENT, "mongodb");
        data.put(HealthIndicatorConstants.DETAIL_MONGO_DATABASE, databaseName());
        String host = host();
        if (Objects.nonNull(host) && !host.isBlank()) {
            data.put(HealthIndicatorConstants.DETAIL_MONGO_HOST, host);
        }
        return data;
    }

    public static String hostFromConnectionString(String connectionString) {
        if (Objects.isNull(connectionString) || connectionString.isBlank()) {
            return HOST_NOT_AVAILABLE;
        }
        try {
            List<String> hosts = new ConnectionString(connectionString).getHosts();
            return hosts.isEmpty() ? HOST_NOT_AVAILABLE : String.join(",", hosts);
        } catch (RuntimeException exception) {
            return HOST_NOT_AVAILABLE;
        }
    }
}
