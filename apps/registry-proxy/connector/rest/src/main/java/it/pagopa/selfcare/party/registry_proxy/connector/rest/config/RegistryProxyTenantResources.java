package it.pagopa.selfcare.party.registry_proxy.connector.rest.config;

import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantDefinition;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RegistryProxyTenantResources {

  private static final Pattern ENV_VAR = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
  private static final Pattern INDEX = Pattern.compile("[a-z0-9][a-z0-9-]*");
  private final TenantRegistry registry;
  private final TenantContext context;
  private final Environment environment;

  public RegistryProxyTenantResources(
      TenantRegistry registry, TenantContext context, Environment environment) {
    this.registry = registry;
    this.context = context;
    this.environment = environment;
  }

  @PostConstruct
  void initialize() {
    if (registry.supportedTenantIds().size() > 1
        && !environment.getProperty("jwt.tenant-claim-required", Boolean.class, false)) {
      throw new IllegalStateException("Shared deployments require the JWT tenant claim");
    }
    Set<String> indexes = new HashSet<>();
    for (String tenant : registry.supportedTenantIds()) {
      if (registry.resolve(tenant).jwt() == null) {
        throw new IllegalStateException("Missing JWT configuration for tenant " + tenant);
      }
      var jwt = registry.resolve(tenant).jwt();
      if (registry.supportedTenantIds().size() > 1
          && (jwt.issuers().isEmpty() || jwt.audiences().isEmpty())) {
        throw new IllegalStateException("Shared deployments require tenant JWT issuers and audiences");
      }
      TenantDefinition.RegistryProxyDefinition resources = registry.resolve(tenant).registryProxy();
      if (resources == null) {
        throw new IllegalStateException("Missing registry-proxy configuration for tenant " + tenant);
      }
      if (resources.search() != null) {
        var search = resources.search();
        URI endpoint = endpoint(search.endpoint());
        if (search.onboardingIndex() == null || !INDEX.matcher(search.onboardingIndex()).matches()) {
          throw new IllegalStateException("Invalid onboarding index for tenant " + tenant);
        }
        if (!indexes.add(endpoint.toString().replaceAll("/+$", "") + "/" + search.onboardingIndex())) {
          throw new IllegalStateException("Onboarding indexes must be distinct per tenant");
        }
        secret(search.apiKeyEnvVar());
      }
      if (resources.nationalRegistries() != null) {
        endpoint(resources.nationalRegistries().endpoint());
        secret(resources.nationalRegistries().apiKeyEnvVar());
      }
      validatePdnd(resources.pdnd());
      validatePdnd(resources.pdndInvitalia());
      if (!resources.invitaliaProducts().isEmpty() && resources.pdndInvitalia() == null) {
        throw new IllegalStateException("Missing Invitalia PDND configuration for tenant " + tenant);
      }
    }
  }

  public TenantDefinition.RegistryProxyDefinition current() {
    var resources = registry.resolve(context.requiredTenantId()).registryProxy();
    if (resources == null) {
      throw new IllegalStateException("Registry-proxy resources are not configured");
    }
    return resources;
  }

  public TenantDefinition.SearchDefinition search() {
    var search = current().search();
    if (search == null) {
      throw new IllegalStateException("Onboarding search is not configured for the current tenant");
    }
    return search;
  }

  public String secret(String reference) {
    if (reference == null || !ENV_VAR.matcher(reference).matches()) {
      throw new IllegalStateException("Invalid registry-proxy credential environment reference");
    }
    String value = environment.getProperty(reference);
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException("Missing registry-proxy credential");
    }
    return value;
  }

  public static URI endpoint(String value) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalStateException("Missing registry-proxy endpoint");
    }
    URI uri = URI.create(value);
    if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
        || uri.getQuery() != null || uri.getFragment() != null) {
      throw new IllegalStateException("Registry-proxy endpoints must be trusted HTTPS URLs");
    }
    return uri;
  }

  private void validatePdnd(TenantDefinition.PdndDefinition pdnd) {
    if (pdnd != null) {
      secret(pdnd.clientIdEnvVar());
      secret(pdnd.privateKeyEnvVar());
      secret(pdnd.kidEnvVar());
      secret(pdnd.purposeIdEnvVar());
      if (!StringUtils.hasText(pdnd.audience())) {
        throw new IllegalStateException("Missing PDND audience");
      }
    }
  }
}
