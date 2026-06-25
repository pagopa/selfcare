package it.pagopa.selfcare.delegation.event.config;

import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ConfigUtilsBean {
    public List<String> getProfiles() {
        return ConfigUtils.getProfiles();
    }
}
