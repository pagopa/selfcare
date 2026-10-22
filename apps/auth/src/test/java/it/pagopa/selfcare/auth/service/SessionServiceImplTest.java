package it.pagopa.selfcare.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.auth.conf.TenantDefinition;
import it.pagopa.selfcare.auth.conf.TenantRegistry;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

  private static final String GLOBAL_AUDIENCE = "api.dev.selfcare.pagopa.it";
  private static final String PNPG_AUDIENCE = "pnpg.dev.selfcare.pagopa.it";

  @Mock TenantRegistry tenantRegistry;

  private SessionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new SessionServiceImpl();
    service.audience = GLOBAL_AUDIENCE;
    service.tenantRegistry = tenantRegistry;
  }

  @Test
  void useGlobalAudienceWhenTenantAudienceDisabled() {
    service.tenantAudienceEnabled = false;

    assertEquals(GLOBAL_AUDIENCE, service.resolveAudience("PNPG"));
    verifyNoInteractions(tenantRegistry);
  }

  @Test
  void useTenantAudienceWhenEnabled() {
    service.tenantAudienceEnabled = true;
    mockTenant(jwt(new TenantDefinition.SessionDefinition("PK", "KID", PNPG_AUDIENCE)));

    assertEquals(PNPG_AUDIENCE, service.resolveAudience("PNPG"));
  }

  @Test
  void fallBackToGlobalAudienceWhenTenantAudienceIsMissing() {
    service.tenantAudienceEnabled = true;
    mockTenant(jwt(new TenantDefinition.SessionDefinition("PK", "KID")));

    assertEquals(GLOBAL_AUDIENCE, service.resolveAudience("PNPG"));
  }

  @Test
  void fallBackToGlobalAudienceWhenTenantAudienceIsBlank() {
    service.tenantAudienceEnabled = true;
    mockTenant(jwt(new TenantDefinition.SessionDefinition("PK", "KID", "  ")));

    assertEquals(GLOBAL_AUDIENCE, service.resolveAudience("PNPG"));
  }

  @Test
  void fallBackToGlobalAudienceWhenSessionIsMissing() {
    service.tenantAudienceEnabled = true;
    mockTenant(jwt(null));

    assertEquals(GLOBAL_AUDIENCE, service.resolveAudience("PNPG"));
  }

  @Test
  void fallBackToGlobalAudienceWhenJwtIsMissing() {
    service.tenantAudienceEnabled = true;
    mockTenant(null);

    assertEquals(GLOBAL_AUDIENCE, service.resolveAudience("PNPG"));
  }

  @Test
  void propagateTenantResolutionFailureWhenEnabled() {
    service.tenantAudienceEnabled = true;
    when(tenantRegistry.resolveEnabledTenant("PNPG"))
        .thenThrow(new ForbiddenException("Tenant is not enabled for auth"));

    assertThrows(ForbiddenException.class, () -> service.resolveAudience("PNPG"));
  }

  private void mockTenant(TenantDefinition.JwtDefinition jwt) {
    TenantDefinition definition =
        new TenantDefinition(null, null, List.of(), "HUB_SPID_LOGIN", true, jwt);
    when(tenantRegistry.resolveEnabledTenant("PNPG"))
        .thenReturn(new TenantRegistry.Tenant("PNPG", definition));
  }

  private static TenantDefinition.JwtDefinition jwt(TenantDefinition.SessionDefinition session) {
    return new TenantDefinition.JwtDefinition("JWT_PUBLIC_KEY_PNPG", session);
  }
}
