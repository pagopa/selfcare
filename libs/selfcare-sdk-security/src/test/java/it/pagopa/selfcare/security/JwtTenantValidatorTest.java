package it.pagopa.selfcare.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtTenantValidatorTest {

  @Mock JsonWebToken jwt;

  private static JwtTenantValidator validator(Map<String, String> environment) {
    return JwtTenantValidator.fromEnvironment(environment::get);
  }

  @Test
  void missingClaimDefaultsToPnpgWhenClaimIsOptional() {
    when(jwt.getClaim("tenant_id")).thenReturn(null);

    assertEquals("PNPG", validator(Map.of()).resolveTokenTenant(jwt));
  }

  @Test
  void missingClaimIsRejectedWhenClaimIsRequired() {
    when(jwt.getClaim("tenant_id")).thenReturn(null);
    JwtTenantValidator validator = validator(Map.of("JWT_TENANT_CLAIM_REQUIRED", "true"));

    assertThrows(TenantValidationException.class, () -> validator.resolveTokenTenant(jwt));
  }

  @Test
  void claimIsAcceptedWhenClaimIsRequired() {
    when(jwt.getClaim("tenant_id")).thenReturn("AR");
    JwtTenantValidator validator = validator(Map.of("JWT_TENANT_CLAIM_REQUIRED", "TRUE"));

    assertEquals("AR", validator.resolveTokenTenant(jwt));
  }

  @Test
  void claimIsNormalized() {
    when(jwt.getClaim("tenant_id")).thenReturn(" ar ");

    assertEquals("AR", validator(Map.of()).resolveTokenTenant(jwt));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "XX"})
  void blankOrUnsupportedClaimIsRejected(String claim) {
    when(jwt.getClaim("tenant_id")).thenReturn(claim);
    JwtTenantValidator validator = validator(Map.of());

    assertThrows(TenantValidationException.class, () -> validator.resolveTokenTenant(jwt));
  }

  @Test
  void nonStringClaimIsRejected() {
    when(jwt.getClaim("tenant_id")).thenReturn(1);
    JwtTenantValidator validator = validator(Map.of());

    assertThrows(TenantValidationException.class, () -> validator.resolveTokenTenant(jwt));
  }

  @ParameterizedTest
  @ValueSource(strings = {"AR", "ar", " Ar "})
  void headerIsComparedAfterNormalization(String header) {
    JwtTenantValidator validator = validator(Map.of());

    assertDoesNotThrow(() -> validator.validateHeader("AR", header));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  ", "PNPG", "AR,AR", "XX"})
  void missingMismatchingOrDuplicatedHeaderIsRejected(String header) {
    JwtTenantValidator validator = validator(Map.of());

    assertThrows(TenantValidationException.class, () -> validator.validateHeader("AR", header));
  }

  @Test
  void environmentTenantsAreNormalized() {
    when(jwt.getClaim("tenant_id")).thenReturn(null);
    JwtTenantValidator validator =
        validator(Map.of("DEFAULT_TENANT", " ar ", "SUPPORTED_TENANTS", " ar , pnpg ,"));

    assertEquals("AR", validator.resolveTokenTenant(jwt));
  }

  @Test
  void defaultTenantMustBeSupported() {
    Map<String, String> environment = Map.of("DEFAULT_TENANT", "PNPG", "SUPPORTED_TENANTS", "AR");

    assertThrows(IllegalArgumentException.class, () -> validator(environment));
  }

  @Test
  void supportedTenantsMustNotBeEmpty() {
    Map<String, String> environment = Map.of("SUPPORTED_TENANTS", " , ");

    assertThrows(IllegalArgumentException.class, () -> validator(environment));
  }

  @Test
  void claimRequiredFlagMustBeABoolean() {
    Map<String, String> environment = Map.of("JWT_TENANT_CLAIM_REQUIRED", "yes");

    assertThrows(IllegalArgumentException.class, () -> validator(environment));
  }
}
