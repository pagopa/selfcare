package it.pagopa.selfcare.auth.service;

import io.quarkus.runtime.Startup;
import io.smallrye.jwt.build.Jwt;
import it.pagopa.selfcare.auth.conf.MachineTokenConfig;
import it.pagopa.selfcare.auth.conf.TenantDefinition.MachineDefinition;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.conf.TenantSessionKeyProvider;
import it.pagopa.selfcare.auth.controller.request.MachineTokenRequest;
import it.pagopa.selfcare.auth.controller.response.MachineTokenResponse;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.InvalidRequestException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;
import org.eclipse.microprofile.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@ApplicationScoped
public class MachineTokenService {
  private static final Logger LOGGER = LoggerFactory.getLogger(MachineTokenService.class);
  public static final String ISSUER = "PAGOPA";

  @Inject MachineTokenConfig config;
  @Inject TenantSessionKeyProvider keys;
  @Inject TenantRegistry tenants;
  @Inject SessionServiceImpl sessions;
  @Inject Config runtimeConfig;

  @PostConstruct
  void initialize() {
    if (!config.enabled()) {
      return;
    }
    if (config.allowedTenants().isEmpty()
        || config.allowedTenants().stream()
            .anyMatch(tenant -> !tenant.matches("[A-Za-z0-9_-]{1,64}"))) {
      throw new IllegalStateException("Invalid M2M allowed tenants configuration");
    }
    for (TenantRegistry.Tenant tenant : tenants.machineTenants()) {
      MachineDefinition machine = tenant.definition().m2m();
      if (!config.allowedTenants().contains(tenant.id())
          || machine.lifetimeSeconds() < 60
          || machine.lifetimeSeconds() > 900
          || machine.groups().size() > 32
          || machine.groups().stream().anyMatch(group -> !group.matches("[A-Za-z0-9._:-]{1,64}"))) {
        throw new IllegalStateException("Invalid M2M configuration for tenant " + tenant.id());
      }
      resolveUid(tenant);
      keys.getSigningKey(tenant.id());
    }
  }

  public MachineTokenResponse issue(MachineTokenRequest request) {
    if (!config.enabled()) {
      throw new ResourceNotFoundException("Resource not found");
    }
    if (request.tenantId() == null || !config.allowedTenants().contains(request.tenantId())) {
      throw new InvalidRequestException("Unknown tenant");
    }
    TenantRegistry.Tenant tenant = tenants.resolveTenant(request.tenantId());
    MachineDefinition machine = tenant.definition().m2m();
    if (machine == null || !machine.enabled()) {
      throw new ForbiddenException("Machine token issuance is not enabled for tenant");
    }
    String uid = resolveUid(tenant);
    String token = sign(tenant, uid, machine.lifetimeSeconds());
    LOGGER.info("event=machine_token_issued tenant={}", tenant.id());
    return new MachineTokenResponse(token, "Bearer", machine.lifetimeSeconds());
  }

  private String resolveUid(TenantRegistry.Tenant tenant) {
    String reference = tenant.definition().m2m().uidEnvVar();
    if (reference == null || reference.isBlank()) {
      throw new IllegalStateException("Missing M2M UID reference for tenant " + tenant.id());
    }
    String uid =
        runtimeConfig
            .getOptionalValue(reference, String.class)
            .filter(value -> !value.isBlank())
            .orElseThrow(
                () -> new IllegalStateException("Missing M2M UID for tenant " + tenant.id()));
    if (!uid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
      throw new IllegalStateException("Invalid M2M UID for tenant " + tenant.id());
    }
    return uid;
  }

  private String sign(TenantRegistry.Tenant tenant, String uid, int lifetime) {
    var key = keys.getSigningKey(tenant.id());
    var machine = tenant.definition().m2m();
    Instant now = Instant.now();
    return Jwt.claims()
        .issuer(ISSUER)
        .subject(uid)
        .claim("uid", uid)
        .claim("name", "auth")
        .claim("client_id", "selfcare-m2m")
        .claim("tenant_id", tenant.id())
        .claim("token_use", "service")
        .groups(new HashSet<>(machine.groups()))
        .audience(sessions.resolveAudience(tenant.definition()))
        .claim("jti", UUID.randomUUID().toString())
        .issuedAt(now)
        .expiresAt(now.plusSeconds(lifetime))
        .jws()
        .keyId(key.keyId())
        .sign(key.privateKey());
  }
}
