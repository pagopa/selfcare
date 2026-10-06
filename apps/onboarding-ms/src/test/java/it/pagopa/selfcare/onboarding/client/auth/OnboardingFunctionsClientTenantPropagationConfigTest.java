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
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.junit.jupiter.api.Test;

/**
 * Pins the contract with onboarding-functions: its HTTP triggers require {@code X-Tenant-Id}, so every
 * generated client must register {@link TenantHeaderClientRequestFilter}, which forwards it through
 * {@link AuthenticationPropagationHeadersFactory}. A {@code ClientHeadersFactory} cannot be used directly:
 * the generator already registers its own because the spec declares the function key security scheme.
 */
class OnboardingFunctionsClientTenantPropagationConfigTest {

    private static final String FUNCTIONS_API_PACKAGE = "org.openapi.quarkus.onboarding_functions_json.api";
    private static final String MAIN_APPLICATION_PROPERTIES = "src/main/resources/application.properties";

    @Test
    void everyGeneratedOnboardingFunctionsClientRegistersTheTenantPropagatingFilter() throws Exception {
        List<Class<?>> functionsClients = generatedFunctionsClients();

        assertFalse(functionsClients.isEmpty());
        for (Class<?> client : functionsClients) {
            List<Class<?>> providers = Arrays.stream(client.getAnnotationsByType(RegisterProvider.class))
                    .<Class<?>>map(RegisterProvider::value)
                    .toList();
            assertTrue(providers.contains(TenantHeaderClientRequestFilter.class),
                    client.getName() + " does not register the tenant propagating filter");
        }
    }

    @Test
    void onboardingFunctionsSpecIsGeneratedWithTheTenantPropagatingFilter() throws IOException {
        Properties properties = mainApplicationProperties();

        assertEquals(
                "@org.eclipse.microprofile.rest.client.annotation.RegisterProvider("
                        + TenantHeaderClientRequestFilter.class.getName() + ".class)",
                properties.getProperty(
                        "quarkus.openapi-generator.codegen.spec.onboarding_functions_json.additional-api-type-annotations"));
    }

    @Test
    void onboardingFunctionsSpecKeepsFunctionKeyAuthentication() throws IOException {
        Properties properties = mainApplicationProperties();

        assertNotNull(properties.getProperty("quarkus.openapi-generator.onboarding_functions_json.auth.api_key.api-key"));
    }

    // The test classpath shadows application.properties with the test one, so the main file is read from disk.
    private static Properties mainApplicationProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(Path.of(MAIN_APPLICATION_PROPERTIES))) {
            properties.load(stream);
        }
        return properties;
    }

    private static List<Class<?>> generatedFunctionsClients() throws IOException, URISyntaxException, ClassNotFoundException {
        URL packageUrl = OnboardingFunctionsClientTenantPropagationConfigTest.class.getClassLoader()
                .getResource(FUNCTIONS_API_PACKAGE.replace('.', '/'));
        assertNotNull(packageUrl, "Generated onboarding-functions clients are missing from the classpath");
        assertEquals("file", packageUrl.getProtocol());

        List<String> classNames;
        try (Stream<Path> files = Files.list(Path.of(packageUrl.toURI()))) {
            classNames = files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith("Api.class"))
                    .map(name -> FUNCTIONS_API_PACKAGE + "." + name.substring(0, name.length() - ".class".length()))
                    .sorted()
                    .toList();
        }
        assertTrue(classNames.contains(FUNCTIONS_API_PACKAGE + ".OrchestrationApi"));

        List<Class<?>> clients = new ArrayList<>();
        for (String className : classNames) {
            clients.add(Class.forName(className));
        }
        return clients;
    }
}
