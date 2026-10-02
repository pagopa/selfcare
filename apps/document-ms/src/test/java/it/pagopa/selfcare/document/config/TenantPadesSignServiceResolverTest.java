package it.pagopa.selfcare.document.config;

import it.pagopa.selfcare.onboarding.crypto.NamiralSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.client.NamirialHttpClient;
import it.pagopa.selfcare.tenant.TenantRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class TenantPadesSignServiceResolverTest {

  @Test
  void resolve_usesArNamirialCredentialsFromTenantRegistry() {
    TenantRegistry tenantRegistry = Mockito.mock(TenantRegistry.class);
    TenantRegistry.NamirialSignatureCredentials namirial =
        new TenantRegistry.NamirialSignatureCredentials(
            "https://namirial.ar.example", "ar-user", "ar-password");
    TenantRegistry.SignatureCredentials signature =
        new TenantRegistry.SignatureCredentials(
            "namirial",
            "PagoPA S.p.A.",
            "Roma",
            "Firma AR",
            Optional.of(namirial),
            Optional.empty());
    when(tenantRegistry.normalizeTenantId("AR")).thenReturn("AR");
    when(tenantRegistry.signatureCredentials("AR")).thenReturn(Optional.of(signature));

    List<List<?>> httpClientArgs = new ArrayList<>();
    List<List<?>> signServiceArgs = new ArrayList<>();
    try (MockedConstruction<NamirialHttpClient> ignoredClient =
            Mockito.mockConstruction(
                NamirialHttpClient.class,
                (mock, context) -> httpClientArgs.add(context.arguments()));
        MockedConstruction<NamiralSignServiceImpl> ignoredSignService =
            Mockito.mockConstruction(
                NamiralSignServiceImpl.class,
                (mock, context) -> signServiceArgs.add(context.arguments()))) {

      TenantPadesSignServiceResolver.ResolvedPadesSignService resolved =
          new TenantPadesSignServiceResolver(tenantRegistry).resolve("AR");

      assertThat(resolved.tenantId()).isEqualTo("AR");
      assertThat(resolved.source()).isEqualTo("namirial");
      assertThat(resolved.signer()).isEqualTo("PagoPA S.p.A.");
      assertThat(resolved.location()).isEqualTo("Roma");
      assertThat(resolved.reason()).isEqualTo("Firma AR");
      assertThat(httpClientArgs).hasSize(1);
      assertThat(httpClientArgs.get(0)).isEqualTo(List.of("https://namirial.ar.example"));
      assertThat(signServiceArgs).hasSize(1);
      assertThat(signServiceArgs.get(0)).satisfies(arguments -> {
        assertThat(arguments.get(1)).isEqualTo("ar-user");
        assertThat(arguments.get(2)).isEqualTo("ar-password");
      });
    }
  }

  @Test
  void resolve_failsWhenTenantHasNoSignatureSection() {
    TenantRegistry tenantRegistry = Mockito.mock(TenantRegistry.class);
    when(tenantRegistry.normalizeTenantId("PNPG")).thenReturn("PNPG");
    when(tenantRegistry.signatureCredentials("PNPG")).thenReturn(Optional.empty());

    TenantPadesSignServiceResolver resolver = new TenantPadesSignServiceResolver(tenantRegistry);

    assertThatThrownBy(() -> resolver.resolve("PNPG"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PagoPA signature is not configured for tenant PNPG");
  }

  @Test
  void signatureToString_doesNotExposeSecrets() {
    TenantRegistry.NamirialSignatureCredentials namirial =
        new TenantRegistry.NamirialSignatureCredentials(
            "https://namirial.ar.example", "ar-user", "ar-password");
    TenantRegistry.SignatureCredentials signature =
        new TenantRegistry.SignatureCredentials(
            "namirial",
            "PagoPA S.p.A.",
            "Roma",
            "Firma AR",
            Optional.of(namirial),
            Optional.empty());

    assertThat(signature).hasToString(
        "SignatureCredentials[source=namirial, signer=PagoPA S.p.A., location=Roma, reason=Firma AR, credentials=REDACTED]");
    assertThat(signature.toString()).doesNotContain("ar-user", "ar-password");
    assertThat(namirial.toString()).doesNotContain("ar-user", "ar-password");
  }
}
