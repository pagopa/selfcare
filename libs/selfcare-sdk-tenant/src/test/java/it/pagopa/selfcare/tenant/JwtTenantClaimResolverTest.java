package it.pagopa.selfcare.tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.pagopa.selfcare.tenant.JwtTenantClaimResolver.Resolution;
import it.pagopa.selfcare.tenant.JwtTenantClaimResolver.Source;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JwtTenantClaimResolverTest {

  private static final List<String> SUPPORTED = List.of("AR", "PNPG");

  @Test
  void missingClaimResolvesToDefaultTenant() {
    assertEquals(
        new Resolution("PNPG", Source.DEFAULT),
        JwtTenantClaimResolver.of(" pnpg ", SUPPORTED, false).resolve(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void missingClaimWithoutDefaultTenantIsRejected(String defaultTenant) {
    JwtTenantClaimResolver resolver = JwtTenantClaimResolver.of(defaultTenant, SUPPORTED, false);
    assertThrows(InvalidTenantClaimException.class, () -> resolver.resolve(null));
    assertThrows(
        InvalidTenantClaimException.class,
        () -> JwtTenantClaimResolver.of(null, SUPPORTED, false).resolve(null));
  }

  @Test
  void missingClaimIsRejectedWhenRequired() {
    JwtTenantClaimResolver resolver = JwtTenantClaimResolver.of("PNPG", SUPPORTED, true);
    assertThrows(InvalidTenantClaimException.class, () -> resolver.resolve(null));
  }

  @Test
  void claimIsNormalized() {
    assertEquals(
        new Resolution("AR", Source.CLAIM),
        JwtTenantClaimResolver.of("PNPG", List.of(" ar ", "pnpg"), true).resolve(" ar "));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "XX"})
  void blankOrUnsupportedClaimIsRejected(String claim) {
    JwtTenantClaimResolver resolver = JwtTenantClaimResolver.of("AR", List.of("AR"), false);
    assertThrows(InvalidTenantClaimException.class, () -> resolver.resolve(claim));
    assertThrows(InvalidTenantClaimException.class, () -> resolver.resolve("PNPG"));
  }

  @Test
  void nonStringClaimIsRejected() {
    JwtTenantClaimResolver resolver = JwtTenantClaimResolver.of("AR", SUPPORTED, false);
    assertThrows(InvalidTenantClaimException.class, () -> resolver.resolve(1));
  }

  @Test
  void defaultTenantMustBeSupported() {
    assertThrows(
        IllegalArgumentException.class, () -> JwtTenantClaimResolver.of("PNPG", List.of("AR"), false));
  }

  @Test
  void supportedTenantsMustNotBeEmpty() {
    assertThrows(
        IllegalArgumentException.class, () -> JwtTenantClaimResolver.of(null, List.of(" "), false));
  }
}
