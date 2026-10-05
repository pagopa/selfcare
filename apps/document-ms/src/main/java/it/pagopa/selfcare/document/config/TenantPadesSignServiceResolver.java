package it.pagopa.selfcare.document.config;

import it.pagopa.selfcare.onboarding.crypto.ArubaPkcs7HashSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.ArubaSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.NamiralSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.NamirialPkcs7HashSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.PadesSignService;
import it.pagopa.selfcare.onboarding.crypto.PadesSignServiceImpl;
import it.pagopa.selfcare.onboarding.crypto.Pkcs7HashSignService;
import it.pagopa.selfcare.onboarding.crypto.client.NamirialHttpClient;
import it.pagopa.selfcare.onboarding.crypto.config.ArubaInitializer;
import it.pagopa.selfcare.onboarding.crypto.config.ArubaSignConfig;
import it.pagopa.selfcare.onboarding.crypto.soap.aruba.sign.generated.client.Auth;
import it.pagopa.selfcare.tenant.TenantRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class TenantPadesSignServiceResolver {

  public static final String SIGNATURE_SOURCE_ARUBA = "aruba";
  public static final String SIGNATURE_SOURCE_NAMIRIAL = "namirial";
  public static final String SIGNATURE_SOURCE_DISABLED = "disabled";

  private final TenantRegistry tenantRegistry;
  private final Map<String, PadesSignService> services = new ConcurrentHashMap<>();

  public TenantPadesSignServiceResolver(TenantRegistry tenantRegistry) {
    this.tenantRegistry = tenantRegistry;
  }

  public ResolvedPadesSignService resolve(String tenantId) {
    String normalizedTenantId = tenantRegistry.normalizeTenantId(tenantId);
    TenantRegistry.SignatureCredentials signature =
        tenantRegistry
            .signatureCredentials(normalizedTenantId)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "PagoPA signature is not configured for tenant " + normalizedTenantId));

    PadesSignService service =
        services.computeIfAbsent(normalizedTenantId, ignored -> createService(normalizedTenantId, signature));
    return new ResolvedPadesSignService(
        normalizedTenantId,
        signature.source(),
        signature.signer(),
        signature.location(),
        signature.reason(),
        service);
  }

  private PadesSignService createService(String tenantId, TenantRegistry.SignatureCredentials signature) {
    String source = signature.source().toLowerCase(Locale.ROOT);
    log.info("PagoPA signature source for tenant {} is {}", tenantId, source);
    return switch (source) {
      case SIGNATURE_SOURCE_ARUBA -> new PadesSignServiceImpl(arubaPkcs7HashSignService(signature));
      case SIGNATURE_SOURCE_NAMIRIAL -> new PadesSignServiceImpl(namirialPkcs7HashSignService(signature));
      case SIGNATURE_SOURCE_DISABLED -> new PadesSignServiceImpl(disabledPkcs7HashSignService());
      default -> throw new IllegalStateException(
          "Unsupported PagoPA signature source " + signature.source() + " for tenant " + tenantId);
    };
  }

  private Pkcs7HashSignService arubaPkcs7HashSignService(
      TenantRegistry.SignatureCredentials signature) {
    TenantRegistry.ArubaSignatureCredentials credentials =
        signature.aruba().orElseThrow(() -> new IllegalStateException("Missing Aruba credentials"));
    Auth auth = new Auth();
    auth.setTypeOtpAuth(credentials.typeOtpAuth());
    auth.setOtpPwd(credentials.otpPwd());
    auth.setUser(credentials.user());
    auth.setDelegatedUser(credentials.delegatedUser());
    auth.setDelegatedPassword(credentials.delegatedPassword());
    auth.setDelegatedDomain(credentials.delegatedDomain());
    ArubaSignConfig config =
        ArubaInitializer.initializeConfig(
            credentials.baseUrl(),
            credentials.connectTimeoutMs(),
            credentials.requestTimeoutMs(),
            auth);
    return new ArubaPkcs7HashSignServiceImpl(new ArubaSignServiceImpl(config));
  }

  private Pkcs7HashSignService namirialPkcs7HashSignService(
      TenantRegistry.SignatureCredentials signature) {
    TenantRegistry.NamirialSignatureCredentials credentials =
        signature.namirial().orElseThrow(() -> new IllegalStateException("Missing Namirial credentials"));
    return new NamirialPkcs7HashSignServiceImpl(
        new NamiralSignServiceImpl(
            new NamirialHttpClient(credentials.baseUrl()),
            credentials.username(),
            credentials.password()));
  }

  private Pkcs7HashSignService disabledPkcs7HashSignService() {
    return new Pkcs7HashSignService() {
      @Override
      public boolean returnsFullPdf() {
        return false;
      }

      @Override
      public byte[] sign(InputStream inputStream) {
        log.info("Signature source is disabled, skipping signing input file");
        return new byte[0];
      }
    };
  }

  public record ResolvedPadesSignService(
      String tenantId,
      String source,
      String signer,
      String location,
      String reason,
      PadesSignService service) {

    public boolean disabled() {
      return SIGNATURE_SOURCE_DISABLED.equals(source);
    }

    @Override
    public String toString() {
      return "ResolvedPadesSignService[tenantId=" + tenantId + ", source=" + source + "]";
    }
  }
}
