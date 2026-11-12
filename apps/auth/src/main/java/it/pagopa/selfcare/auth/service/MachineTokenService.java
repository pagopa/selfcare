package it.pagopa.selfcare.auth.service;

import io.quarkus.runtime.Startup;
import io.smallrye.jwt.build.Jwt;
import it.pagopa.selfcare.auth.conf.MachineTokenConfig;
import it.pagopa.selfcare.auth.conf.TenantDefinition.MachineDefinition;
import it.pagopa.selfcare.auth.conf.TenantDefinition.MachineGrant;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.conf.TenantSessionKeyProvider;
import it.pagopa.selfcare.auth.controller.request.MachineTokenRequest;
import it.pagopa.selfcare.auth.controller.response.MachineTokenResponse;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import it.pagopa.selfcare.auth.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
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
  @Inject MachineIamAuthorizer iam;

  @PostConstruct
  void initialize() {
    if (!config.enabled()) {
      return;
    }
    for (TenantRegistry.Tenant tenant : tenants.machineTenants()) {
      MachineDefinition machine = tenant.definition().m2m();
      if (!tenant.id().matches("AR|PNPG")
          || machine.uid() == null
          || !UUID.fromString(machine.uid()).toString().equals(machine.uid())
          || machine.lifetimeSeconds() < 60
          || machine.lifetimeSeconds() > 900
          || machine.groups().size() > 32
          || machine.groups().stream().anyMatch(group -> !group.matches("[A-Za-z0-9._:-]{1,64}"))) {
        throw new IllegalStateException("Invalid M2M configuration for tenant " + tenant.id());
      }
      MachineIamAuthorizer.requireHttps(machine.iamBaseUrl());
      var requests = new HashSet<List<String>>();
      for (MachineGrant grant : machine.grants()) {
        if (grant.audience() == null
            || !grant.audience().matches("[a-z][a-z0-9-]{0,63}")
            || grant.scope() == null
            || !grant.scope().matches("[a-z][a-z0-9.:-]{0,127}")
            || grant.productId() == null
            || !grant.productId().matches("[a-zA-Z0-9._-]{1,128}")
            || !requests.add(List.of(grant.audience(), grant.scope()))) {
          throw new IllegalStateException("Invalid M2M grant for tenant " + tenant.id());
        }
      }
      keys.getSigningKey(tenant.id());
    }
  }

  public MachineTokenResponse issue(MachineTokenRequest request) {
    if (!config.enabled()) {
      throw new ResourceNotFoundException("Resource not found");
    }
    TenantRegistry.Tenant tenant = tenants.resolveTenant(request.tenantId());
    MachineDefinition machine = tenant.definition().m2m();
    if (machine == null || !machine.enabled()) {
      throw new ForbiddenException("Machine token issuance is not enabled for tenant");
    }
    MachineGrant grant =
        machine.grants().stream()
            .filter(
                value ->
                    value.audience().equals(request.audience())
                        && value.scope().equals(request.scope()))
            .findFirst()
            .orElseThrow(() -> new ForbiddenException("Machine token request is not authorized"));

    // IAM requires a signed identity. This short-lived bootstrap token is never returned to
    // callers.
    String bootstrap = sign(tenant, grant.scope(), 30);
    iam.requirePermission(tenant.id(), machine, grant, bootstrap);
    String token = sign(tenant, grant.scope(), machine.lifetimeSeconds());
    LOGGER.info(
        "event=machine_token_issued tenant={} destination={} scope={}",
        tenant.id(),
        grant.audience(),
        grant.scope());
    return new MachineTokenResponse(token, "Bearer", machine.lifetimeSeconds(), grant.scope());
  }

  private String sign(TenantRegistry.Tenant tenant, String scope, int lifetime) {
    var key = keys.getSigningKey(tenant.id());
    var machine = tenant.definition().m2m();
    Instant now = Instant.now();
    return Jwt.claims()
        .issuer(ISSUER)
        .subject(machine.uid())
        .claim("uid", machine.uid())
        .claim("name", "auth")
        .claim("client_id", "selfcare-m2m")
        .claim("tenant_id", tenant.id())
        .claim("token_use", "service")
        .claim("scope", scope)
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
