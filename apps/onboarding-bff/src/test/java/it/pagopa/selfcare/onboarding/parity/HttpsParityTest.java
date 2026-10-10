package it.pagopa.selfcare.onboarding.parity;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(HttpsParityTest.Profile.class)
class HttpsParityTest {

    private static final String HSTS = "max-age=31536000 ; includeSubDomains";
    private static final String KEYSTORE_PASSWORD = "parity-test-only";

    @TestHTTPResource(tls = true)
    URI secureBase;

    @ParityTestEnvironment.Stub
    DownstreamStub stub;

    Path keyStore;
    SSLContext sslContext;

    @Test
    void quarkusUsesHstsOnlyForSecuredHttpsRoutes() {
        runScenarios(secureBase.toString(), client());
    }

    @Test
    void unchangedSpringUsesTheSameHttpsContract() throws Exception {
        String jar = System.getProperty(SpringReference.JAR_PROPERTY);
        assumeTrue(jar != null && !jar.isBlank(), "Supply -Dparity.spring.jar to verify the HTTPS oracle");
        HttpClient client = client();
        try (SpringReference spring = SpringReference.startSecure(Path.of(jar), stub, keyStore, KEYSTORE_PASSWORD, client)) {
            runScenarios(spring.baseUrl(), client);
        }
    }

    private HttpClient client() {
        return HttpClient.newBuilder().sslContext(sslContext).connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    private void runScenarios(String baseUrl, HttpClient client) {
        for (Scenario scenario : List.of(
                Scenario.api("https", "success", "/v1/products")
                        .stub(s -> s.on(DownstreamStub.MS_PRODUCT, "GET", "/product", DownstreamStub.Reply.json(200, "[]")))
                        .expect(c -> c.status(200).header("Strict-Transport-Security", HSTS).totalCalls(1).propagatesIdentity()),
                Scenario.of("https", "unauthorized", "GET", "/v1/products")
                        .expect(c -> c.problem(401, null).header("Strict-Transport-Security", HSTS).totalCalls(0)),
                Scenario.api("https", "validation", "POST", "/v1/users/search-user").json("{}")
                        .expect(c -> c.status(400).header("Strict-Transport-Security", HSTS).totalCalls(0)),
                Scenario.of("https", "public-health", "GET", "/actuator/health")
                        .expect(c -> c.status(200).headerAbsent("Strict-Transport-Security").totalCalls(0)))) {
            scenario.run(baseUrl, stub, client);
        }
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public List<TestResourceEntry> testResources() {
            return List.of(new TestResourceEntry(Environment.class));
        }
    }

    public static class Environment extends ParityTestEnvironment {
        private Path directory;
        private Path keyStore;
        private SSLContext sslContext;

        @Override
        public Map<String, String> start() {
            Map<String, String> config = new LinkedHashMap<>(super.start());
            try {
                directory = Files.createTempDirectory("bff-https-parity-");
                keyStore = directory.resolve("server.p12");
                Path keytoolLog = directory.resolve("keytool.log");
                Process keytool = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                        "-genkeypair", "-alias", "localhost", "-keyalg", "RSA", "-keysize", "2048",
                        "-validity", "2", "-dname", "CN=localhost", "-ext", "SAN=DNS:localhost,IP:127.0.0.1",
                        "-storetype", "PKCS12", "-keystore", keyStore.toString(),
                        "-storepass", KEYSTORE_PASSWORD, "-keypass", KEYSTORE_PASSWORD, "-noprompt")
                        .redirectErrorStream(true).redirectOutput(keytoolLog.toFile()).start();
                if (!keytool.waitFor(30, TimeUnit.SECONDS)) {
                    keytool.destroyForcibly();
                    throw new IllegalStateException("Timed out creating the HTTPS fixture certificate");
                }
                if (keytool.exitValue() != 0) {
                    throw new IllegalStateException("Cannot create HTTPS certificate: " + Files.readString(keytoolLog));
                }
                KeyStore keys = KeyStore.getInstance("PKCS12");
                try (InputStream input = Files.newInputStream(keyStore)) {
                    keys.load(input, KEYSTORE_PASSWORD.toCharArray());
                }
                KeyStore trusted = KeyStore.getInstance("PKCS12");
                trusted.load(null, null);
                trusted.setCertificateEntry("localhost", keys.getCertificate("localhost"));
                TrustManagerFactory trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trust.init(trusted);
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trust.getTrustManagers(), null);
                config.put("quarkus.http.ssl.certificate.key-store-file", keyStore.toString());
                config.put("quarkus.http.ssl.certificate.key-store-password", KEYSTORE_PASSWORD);
                config.put("quarkus.http.ssl.certificate.key-store-file-type", "PKCS12");
                config.put("quarkus.http.test-ssl-port", "0");
                return config;
            } catch (Exception failure) {
                stop();
                if (failure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("Cannot start HTTPS parity fixtures", failure);
            }
        }

        @Override
        public void inject(TestInjector injector) {
            super.inject(injector);
            injector.injectIntoFields(keyStore, new TestInjector.MatchesType(Path.class));
            injector.injectIntoFields(sslContext, new TestInjector.MatchesType(SSLContext.class));
        }

        @Override
        public void stop() {
            super.stop();
            if (directory != null) {
                try {
                    Files.deleteIfExists(directory.resolve("server.p12"));
                    Files.deleteIfExists(directory.resolve("keytool.log"));
                    Files.deleteIfExists(directory);
                } catch (java.io.IOException failure) {
                    throw new IllegalStateException("Cannot remove HTTPS parity fixtures", failure);
                }
            }
        }
    }
}
