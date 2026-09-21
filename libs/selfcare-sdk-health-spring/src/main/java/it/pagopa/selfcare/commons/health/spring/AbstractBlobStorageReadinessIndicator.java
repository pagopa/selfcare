package it.pagopa.selfcare.commons.health.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractBlobStorageReadinessIndicator extends AbstractReadinessIndicator {

    protected abstract String account();

    protected abstract String container();

    protected String probeTarget() {
        return "";
    }

    @Override
    protected Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>(4);
        data.put(HealthIndicatorConstants.DETAIL_COMPONENT, "blob-storage");
        data.put(HealthIndicatorConstants.DETAIL_BLOB_ACCOUNT, account());
        data.put(HealthIndicatorConstants.DETAIL_BLOB_CONTAINER, container());
        String probeTarget = probeTarget();
        if (Objects.nonNull(probeTarget) && !probeTarget.isBlank()) {
            data.put(HealthIndicatorConstants.DETAIL_BLOB_PROBE_TARGET, probeTarget);
        }
        return data;
    }
}
