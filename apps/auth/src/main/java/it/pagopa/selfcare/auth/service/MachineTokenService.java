package it.pagopa.selfcare.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import io.smallrye.jwt.build.Jwt;
import it.pagopa.selfcare.auth.conf.MachineTokenConfig;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.conf.TenantSessionKeyProvider;
import it.pagopa.selfcare.auth.controller.request.MachineTokenRequest;
import it.pagopa.selfcare.auth.controller.response.MachineTokenResponse;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@ApplicationScoped
public class MachineTokenService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MachineTokenService.class);
  public static final String ISSUER = "SELFCARE-M2M";

  @Inject MachineTokenConfig config;
  @Inject MachineIdentityVerifier verifier;
  @Inject TenantSessionKeyProvider keys;
  @Inject TenantRegistry tenants;
  @Inject ObjectMapper mapper;
  private List<Grant> grants = List.of();

  @PostConstruct
  void initialize() {
    if (!config.enabled()) {
      return;
    }
    if (config.lifetime().compareTo(Duration.ofSeconds(60)) < 0
        || config.lifetime().compareTo(Duration.ofSeconds(900)) > 0) {
      throw new IllegalStateException("M2M lifetime must be between 60 and 900 seconds");
    }
    try {
      grants = List.copyOf(mapper.readValue(config.grants(), new TypeReference<List<Grant>>() {}));
    } catch (JsonProcessingException | NullPointerException exception) {
      throw new IllegalStateException("Invalid M2M grant configuration");
    }
    for (Grant grant : grants) {
      grant.validate();
      tenants.resolveEnabledTenant(grant.tenantId());
    }
    if (grants.stream().map(Grant::requestKey).distinct().count() != grants.size()) {
      throw new IllegalStateException("Duplicate M2M grant");
    }
  }

  public MachineTokenResponse issue(String authorization, MachineTokenRequest request) {
    if (!config.enabled()) {
      throw new ResourceNotFoundException("Resource not found");
    }
    String objectId = verifier.verify(authorization);
    Grant grant =
        grants.stream()
            .filter(
                value ->
                    value.objectId().equals(objectId)
                        && value.tenantId().equals(request.tenantId())
                        && value.audience().equals(request.audience())
                        && value.scope().equals(request.scope()))
            .findFirst()
            .orElseThrow(() -> new ForbiddenException("Machine identity is not authorized"));
    tenants.resolveEnabledTenant(grant.tenantId());
    TenantSessionKeyProvider.SigningKey key = keys.getSigningKey(grant.tenantId());
    Instant now = Instant.now();
    String token =
        Jwt.claims()
            .issuer(ISSUER)
            .subject(grant.serviceId())
            .claim("uid", grant.serviceId())
            .claim("tenant_id", grant.tenantId())
            .claim("token_use", "service")
            .claim("scope", grant.scope())
            .audience(grant.audience())
            .claim("jti", UUID.randomUUID().toString())
            .issuedAt(now)
            .expiresAt(now.plus(config.lifetime()))
            .jws()
            .keyId(key.keyId())
            .sign(key.privateKey());
    LOGGER.info(
        "event=machine_token_issued service={} tenant={} audience={} scope={}",
        grant.serviceId(),
        grant.tenantId(),
        grant.audience(),
        grant.scope());
    return new MachineTokenResponse(token, "Bearer", config.lifetime().getSeconds(), grant.scope());
  }

  record Grant(String objectId, String serviceId, String tenantId, String audience, String scope) {
    void validate() {
      if (objectId == null
          || !UUID.fromString(objectId).toString().equals(objectId)
          || serviceId == null
          || !serviceId.matches("[a-z][a-z0-9-]{0,63}")
          || tenantId == null
          || !tenantId.matches("AR|PNPG")
          || audience == null
          || !audience.matches("[a-z][a-z0-9-]{0,63}")
          || scope == null
          || !scope.matches("[a-z][a-z0-9.:-]{0,127}")) {
        throw new IllegalStateException("Invalid M2M grant");
      }
    }

    List<String> requestKey() {
      return List.of(objectId, tenantId, audience, scope);
    }
  }
}
