package it.pagopa.selfcare.auth.util;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SamlValidatorCertificateTest {

  @Inject
  SamlValidator samlValidator;

  private static final String FAKE_CERT = "-----BEGIN CERTIFICATE-----MIIDDzCCAfegAwIBAgIUTCG4Fbd1yUJgFMfL8ic1JIJji80wDQYJKoZIhvcNAQELBQAwFzEVMBMGA1UEAwwMbWlvc2l0by50ZXN0MB4XDTI1MDkwOTE1MjA1N1oXDTI2MDkwOTE1MjA1N1owFzEVMBMGA1UEAwwMbWlvc2l0by50ZXN0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmk4NOiqV11om20yNg/Tpx10FaNO9fGUDtM14V0gh7e9meSHh/nLVSAK/7uAPGvWUL3xZ++0LUFLsArFLSHBsfull63w95Uoq3PtABqUKSkI2gkbYclzFiIdrrH9rIoMYvYG5zIroKNSD2/iUDfE3Go78AWilfFqa/OvIZ3riNpTyqtAbFyqJ9tBlSgdSVV9LrqdpTRnk92bkaX0DVHwnXay3z0RkWjO8uOD/GGC3B8dDuPUS4wZTwgfS8cguz9SsgI0hMi2RXRmeTiSrAJFlx14PfGKnBsaVnM+JDdZH9KTuSJ7+8d8ld4yWz1q56UVdTw1DIV8OC91mEvhtuYJGvwIDAQABo1MwUTAdBgNVHQ4EFgQUfGEOxC2NY2ONlCj7K8vcmh/utGYwHwYDVR0jBBgwFoAUfGEOxC2NY2ONlCj7K8vcmh/utGYwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEANiCG8BvlOuX9cS3yY4DxvLVv0gzTzwAo157JFFE50+qZ5Hn2vQKNKQOlrSHe9+5Jr2K/sGswRapDVG0o+wV5LlFTcoe6k/DVR196XSzg4Y60KTDQLvs/0dfSZ0tYjn0Sni5TuQrGfJgnwgZ2XkWUHMXKg22Fd6ig/PiZE+fNbyisTByUJlcqjESPP1CkhWhhbsg8VSmv66lyeWNSY7dEJbceG/Zl8z+SmSU2IkYQ2eQu0eOc7MvaaPZOWdubeJefm75nx6jnnQZMWEpL5+wQaB2qKdejRibX0ps0E6lRBOSUqf8Ij3BLMRQ4rPYAvM8eGxbsPRrR5KawuhrPCcU82w==-----END CERTIFICATE-----";

  // Helper class to generate self-signed certificates for testing
  private static class CertificateTestUtils {
    public static X509Certificate generateCertificate() throws CertificateException {
      String cleanedPem = FAKE_CERT
        .replace("-----BEGIN CERTIFICATE-----", "")
        .replace("-----END CERTIFICATE-----", "")
        .replaceAll("\\s", "");

      byte[] certificateBytes = Base64.getDecoder().decode(cleanedPem);

      // 3. Usa una CertificateFactory per generare l'oggetto certificato
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      InputStream is = new ByteArrayInputStream(certificateBytes);

      return (X509Certificate) factory.generateCertificate(is);
    }

    public static X509Certificate generateCertificate(Instant start, Instant end, KeyPair keyPair) throws Exception {
      // This is a simplified version of a self-signed certificate generator
      // In a real project, you might use a library like Bouncy Castle for more options
      String subjectDN = "CN=Test, O=TestOrg, C=IT";
      String command = String.format(
        "keytool -genkeypair -alias testcert -keyalg RSA -keysize 2048 -dname \"%s\" " +
          "-startdate %tF -validity %d -keystore test.keystore -storepass password -keypass password",
        subjectDN, Date.from(start), 1);

      Process process = Runtime.getRuntime().exec(command);
      process.waitFor();

      // Get the class loader
      ClassLoader classLoader = CertificateTestUtils.class.getClassLoader();

      InputStream keystoreStream = classLoader.getResourceAsStream("test.keystore");

      if (keystoreStream == null) {
        throw new FileNotFoundException("Keystore file 'test.keystore' not found in resources!");
      }

      KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(keystoreStream, "password".toCharArray());

      return (X509Certificate) keyStore.getCertificate("miocert");
    }

    public static KeyPair generateKeyPair() throws Exception {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
      kpg.initialize(2048);
      return kpg.generateKeyPair();
    }
  }

  @Test
  void validateCertificate_ValidCertificate_ShouldNotThrowException() throws Exception {
//    X509Certificate validCertificate =  CertificateTestUtils.generateCertificate();

//    // Arrange
//    Instant now = Instant.now();
//    Instant start = now.minus(1, ChronoUnit.DAYS); // Valid from yesterday
//    Instant end = now.plus(1, ChronoUnit.DAYS);   // Valid until tomorrow
//    KeyPair keyPair = CertificateTestUtils.generateKeyPair();
    X509Certificate validCertificate = CertificateTestUtils.generateCertificate();

    // Act & Assert
    assertDoesNotThrow(() -> samlValidator.validateCertificate(validCertificate),
      "A valid certificate should not throw an exception.");
  }

}
