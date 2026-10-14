package it.pagopa.selfcare.document.config;

import it.pagopa.selfcare.onboarding.crypto.ArubaPkcs7HashSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.ArubaSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.NamiralSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.NamirialPkcs7HashSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.PadesSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.Pkcs7HashSignService;
import it.pagopa.selfcare.onboarding.crypto.client.NamirialHttpClient;
import it.pagopa.selfcare.onboarding.crypto.config.ArubaSignConfig;
import it.pagopa.selfcare.tenant.TenantRegistry;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
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
  void resolve_buildsDistinctServicesPerTenantAndCachesThem() {
    TenantRegistry tenantRegistry =
        registryOf(
            Map.of(
                "AR",
                signature(
                    "namirial",
                    "PagoPA AR",
                    "Roma",
                    "Firma AR",
                    Optional.of(namirial("https://namirial.ar.example", "ar-user", "ar-password")),
                    Optional.empty()),
                "PNPG",
                signature(
                    "namirial",
                    "PagoPA PNPG",
                    "Milano",
                    "Firma PNPG",
                    Optional.of(
                        namirial("https://namirial.pnpg.example", "pnpg-user", "pnpg-password")),
                    Optional.empty())));
    TenantPadesSignServiceResolver resolver = new TenantPadesSignServiceResolver(tenantRegistry);

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

      TenantPadesSignServiceResolver.ResolvedPadesSignService ar = resolver.resolve("AR");
      TenantPadesSignServiceResolver.ResolvedPadesSignService pnpg = resolver.resolve("PNPG");

      assertThat(ar.service()).isNotNull().isNotSameAs(pnpg.service());
      assertThat(ar.disabled()).isFalse();
      assertThat(pnpg.disabled()).isFalse();
      assertThat(ar.signer()).isEqualTo("PagoPA AR");
      assertThat(pnpg.signer()).isEqualTo("PagoPA PNPG");
      assertThat(pnpg.location()).isEqualTo("Milano");
      assertThat(pnpg.reason()).isEqualTo("Firma PNPG");

      assertThat(resolver.resolve("AR").service()).isSameAs(ar.service());
      assertThat(resolver.resolve("ar").service()).isSameAs(ar.service());
      assertThat(resolver.resolve("PNPG").service()).isSameAs(pnpg.service());

      assertThat(httpClientArgs)
          .containsExactly(
              List.of("https://namirial.ar.example"), List.of("https://namirial.pnpg.example"));
      assertThat(signServiceArgs).hasSize(2);
      assertThat(List.<Object>copyOf(signServiceArgs.get(0)).subList(1, 3))
          .containsExactly("ar-user", "ar-password");
      assertThat(List.<Object>copyOf(signServiceArgs.get(1)).subList(1, 3))
          .containsExactly("pnpg-user", "pnpg-password");
    }
  }

  @Test
  void resolve_mapsArubaCredentialsToTheArubaSignConfig() throws Exception {
    TenantRegistry.ArubaSignatureCredentials aruba =
        new TenantRegistry.ArubaSignatureCredentials(
            "https://aruba.ar.example/sign",
            1000,
            2000,
            "otp-auth",
            "otp-pwd",
            "aruba-user",
            "delegated-user",
            "delegated-password",
            "delegated-domain");
    TenantRegistry tenantRegistry =
        registryOf(
            Map.of(
                "AR",
                signature("aruba", "PagoPA S.p.A.", "Roma", "Firma AR", Optional.empty(), Optional.of(aruba))));

    List<ArubaSignConfig> configs = new ArrayList<>();
    List<Pkcs7HashSignService> pkcs7Services = new ArrayList<>();
    try (MockedConstruction<ArubaSignServiceImpl> arubaSignService =
            Mockito.mockConstruction(
                ArubaSignServiceImpl.class,
                (mock, context) -> configs.add((ArubaSignConfig) context.arguments().get(0)));
        MockedConstruction<PadesSignServiceImpl> ignoredPades =
            Mockito.mockConstruction(
                PadesSignServiceImpl.class,
                (mock, context) ->
                    pkcs7Services.add((Pkcs7HashSignService) context.arguments().get(0)));
        MockedConstruction<NamirialHttpClient> namirialClient =
            Mockito.mockConstruction(NamirialHttpClient.class)) {

      TenantPadesSignServiceResolver.ResolvedPadesSignService resolved =
          new TenantPadesSignServiceResolver(tenantRegistry).resolve("AR");

      assertThat(resolved.source()).isEqualTo("aruba");
      assertThat(resolved.disabled()).isFalse();
      assertThat(arubaSignService.constructed()).hasSize(1);
      assertThat(namirialClient.constructed()).isEmpty();
      assertThat(pkcs7Services).singleElement().isInstanceOf(ArubaPkcs7HashSignServiceImpl.class);
      assertThat(pkcs7Services.get(0).returnsFullPdf()).isFalse();
      assertThat(configs).hasSize(1);
      ArubaSignConfig config = configs.get(0);
      assertThat(config.getBaseUrl()).isEqualTo("https://aruba.ar.example/sign");
      assertThat(config.getConnectTimeoutMs()).isEqualTo(1000);
      assertThat(config.getRequestTimeoutMs()).isEqualTo(2000);
      assertThat(config.getAuth().getTypeOtpAuth()).isEqualTo("otp-auth");
      assertThat(config.getAuth().getOtpPwd()).isEqualTo("otp-pwd");
      assertThat(config.getAuth().getUser()).isEqualTo("aruba-user");
      assertThat(config.getAuth().getDelegatedUser()).isEqualTo("delegated-user");
      assertThat(config.getAuth().getDelegatedPassword()).isEqualTo("delegated-password");
      assertThat(config.getAuth().getDelegatedDomain()).isEqualTo("delegated-domain");
      assertThat(config.getAuth().getTypeHSM()).isEqualTo("COSIGN");
    }
  }

  @Test
  void resolve_defaultsArubaTimeoutsToZeroWhenNotConfigured() {
    TenantRegistry.ArubaSignatureCredentials aruba =
        new TenantRegistry.ArubaSignatureCredentials(
            "https://aruba.ar.example/sign",
            null,
            null,
            "otp-auth",
            "otp-pwd",
            "aruba-user",
            "delegated-user",
            "delegated-password",
            "delegated-domain");
    TenantRegistry tenantRegistry =
        registryOf(
            Map.of(
                "AR",
                signature("aruba", "PagoPA S.p.A.", "Roma", "Firma AR", Optional.empty(), Optional.of(aruba))));

    List<ArubaSignConfig> configs = new ArrayList<>();
    try (MockedConstruction<ArubaSignServiceImpl> ignoredArubaSignService =
        Mockito.mockConstruction(
            ArubaSignServiceImpl.class,
            (mock, context) -> configs.add((ArubaSignConfig) context.arguments().get(0)))) {

      new TenantPadesSignServiceResolver(tenantRegistry).resolve("AR");

      assertThat(configs).singleElement().satisfies(config -> {
        assertThat(config.getConnectTimeoutMs()).isZero();
        assertThat(config.getRequestTimeoutMs()).isZero();
      });
    }
  }

  @Test
  void resolve_withDisabledSourceBuildsANoOpSignerWithoutVendorClients() throws Exception {
    TenantRegistry tenantRegistry =
        registryOf(
            Map.of(
                "AR",
                signature(
                    "disabled", "PagoPA S.p.A.", "Roma", "Firma AR", Optional.empty(), Optional.empty())));

    List<Pkcs7HashSignService> pkcs7Services = new ArrayList<>();
    try (MockedConstruction<PadesSignServiceImpl> ignoredPades =
            Mockito.mockConstruction(
                PadesSignServiceImpl.class,
                (mock, context) ->
                    pkcs7Services.add((Pkcs7HashSignService) context.arguments().get(0)));
        MockedConstruction<NamirialHttpClient> namirialClient =
            Mockito.mockConstruction(NamirialHttpClient.class);
        MockedConstruction<ArubaSignServiceImpl> arubaSignService =
            Mockito.mockConstruction(ArubaSignServiceImpl.class)) {
      TenantPadesSignServiceResolver resolver = new TenantPadesSignServiceResolver(tenantRegistry);

      TenantPadesSignServiceResolver.ResolvedPadesSignService resolved = resolver.resolve("AR");

      assertThat(resolved.source()).isEqualTo("disabled");
      assertThat(resolved.disabled()).isTrue();
      assertThat(resolved.signer()).isEqualTo("PagoPA S.p.A.");
      assertThat(namirialClient.constructed()).isEmpty();
      assertThat(arubaSignService.constructed()).isEmpty();
      assertThat(resolver.resolve("AR").service()).isSameAs(resolved.service());
      assertThat(pkcs7Services).hasSize(1);
      Pkcs7HashSignService noOp = pkcs7Services.get(0);
      assertThat(noOp).isNotInstanceOf(NamirialPkcs7HashSignServiceImpl.class);
      assertThat(noOp).isNotInstanceOf(ArubaPkcs7HashSignServiceImpl.class);
      assertThat(noOp.returnsFullPdf()).isFalse();
      assertThat(noOp.sign(new ByteArrayInputStream(new byte[] {1, 2, 3}))).isEmpty();
    }
  }

  @Test
  void resolve_rejectsUnsupportedSourceWithoutCachingAService() {
    TenantRegistry tenantRegistry =
        registryOf(
            Map.of(
                "AR",
                signature("gpg", "PagoPA S.p.A.", "Roma", "Firma AR", Optional.empty(), Optional.empty())));
    TenantPadesSignServiceResolver resolver = new TenantPadesSignServiceResolver(tenantRegistry);

    assertThatThrownBy(() -> resolver.resolve("AR"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unsupported PagoPA signature source gpg for tenant AR");
    assertThatThrownBy(() -> resolver.resolve("AR"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Unsupported PagoPA signature source gpg");
  }

  @ParameterizedTest
  @CsvSource({"aruba,Missing Aruba credentials", "namirial,Missing Namirial credentials"})
  void resolve_failsWhenTheSelectedSourceHasNoCredentials(String source, String message) {
    TenantRegistry tenantRegistry =
        registryOf(
            Map.of(
                "AR",
                signature(source, "PagoPA S.p.A.", "Roma", "Firma AR", Optional.empty(), Optional.empty())));
    TenantPadesSignServiceResolver resolver = new TenantPadesSignServiceResolver(tenantRegistry);

    assertThatThrownBy(() -> resolver.resolve("AR"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(message);
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

  private static TenantRegistry registryOf(
      Map<String, TenantRegistry.SignatureCredentials> signatures) {
    TenantRegistry tenantRegistry = Mockito.mock(TenantRegistry.class);
    when(tenantRegistry.normalizeTenantId(anyString()))
        .thenAnswer(invocation -> invocation.<String>getArgument(0).toUpperCase(Locale.ROOT));
    signatures.forEach(
        (tenantId, signature) ->
            when(tenantRegistry.signatureCredentials(tenantId)).thenReturn(Optional.of(signature)));
    return tenantRegistry;
  }

  private static TenantRegistry.SignatureCredentials signature(
      String source,
      String signer,
      String location,
      String reason,
      Optional<TenantRegistry.NamirialSignatureCredentials> namirial,
      Optional<TenantRegistry.ArubaSignatureCredentials> aruba) {
    return new TenantRegistry.SignatureCredentials(
        source, signer, location, reason, namirial, aruba);
  }

  private static TenantRegistry.NamirialSignatureCredentials namirial(
      String baseUrl, String username, String password) {
    return new TenantRegistry.NamirialSignatureCredentials(baseUrl, username, password);
  }
}
