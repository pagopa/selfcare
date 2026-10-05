package it.pagopa.selfcare.onboarding.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.junit.jupiter.api.Test;

/**
 * Pins the contract with document-ms: every generated client must go through
 * {@link AuthenticationPropagationHeadersFactory}, which is what forwards {@code X-Tenant-Id}.
 */
class DocumentClientTenantPropagationConfigTest {

    private static final String DOCUMENT_API_PACKAGE = "org.openapi.quarkus.document_json.api";
    private static final String MAIN_APPLICATION_PROPERTIES = "src/main/resources/application.properties";
    private static final String DOCUMENT_URL = "${MS_DOCUMENT_URL:http://localhost:8080}";

    @Test
    void everyGeneratedDocumentClientRegistersTheTenantPropagatingHeadersFactory() throws Exception {
        List<Class<?>> documentClients = generatedDocumentClients();

        assertFalse(documentClients.isEmpty());
        for (Class<?> client : documentClients) {
            RegisterClientHeaders registerClientHeaders = client.getAnnotation(RegisterClientHeaders.class);
            assertNotNull(registerClientHeaders, client.getName() + " does not register a ClientHeadersFactory");
            assertEquals(AuthenticationPropagationHeadersFactory.class, registerClientHeaders.value(), client.getName());
        }
    }

    @Test
    void documentSpecIsGeneratedWithTheTenantPropagatingHeadersFactory() throws IOException {
        Properties properties = mainApplicationProperties();

        assertEquals(
                "@org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders("
                        + AuthenticationPropagationHeadersFactory.class.getName() + ".class)",
                properties.getProperty("quarkus.openapi-generator.codegen.spec.document_json.additional-api-type-annotations"));
    }

    @Test
    void usedDocumentClientsTargetDocumentMs() throws IOException {
        Properties properties = mainApplicationProperties();

        assertEquals(DOCUMENT_URL, properties.getProperty(
                "quarkus.rest-client.\"" + DOCUMENT_API_PACKAGE + ".DocumentContentControllerApi\".url"));
        assertEquals(DOCUMENT_URL, properties.getProperty(
                "quarkus.rest-client.\"" + DOCUMENT_API_PACKAGE + ".DocumentControllerApi\".url"));
    }

    // The test classpath shadows application.properties with the test one, so the main file is read from disk.
    private static Properties mainApplicationProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(Path.of(MAIN_APPLICATION_PROPERTIES))) {
            properties.load(stream);
        }
        return properties;
    }

    private static List<Class<?>> generatedDocumentClients() throws IOException, URISyntaxException, ClassNotFoundException {
        URL packageUrl = DocumentClientTenantPropagationConfigTest.class.getClassLoader()
                .getResource(DOCUMENT_API_PACKAGE.replace('.', '/'));
        assertNotNull(packageUrl, "Generated document clients are missing from the classpath");
        assertEquals("file", packageUrl.getProtocol());

        List<String> classNames;
        try (Stream<Path> files = Files.list(Path.of(packageUrl.toURI()))) {
            classNames = files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith("Api.class"))
                    .map(name -> DOCUMENT_API_PACKAGE + "." + name.substring(0, name.length() - ".class".length()))
                    .sorted()
                    .toList();
        }
        assertTrue(classNames.contains(DOCUMENT_API_PACKAGE + ".DocumentControllerApi"));
        assertTrue(classNames.contains(DOCUMENT_API_PACKAGE + ".DocumentContentControllerApi"));

        List<Class<?>> clients = new ArrayList<>();
        for (String className : classNames) {
            clients.add(Class.forName(className));
        }
        return clients;
    }
}
