package it.pagopa.selfcare.onboarding.config;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

class OpenApiMetadataTest {

    private final OpenApiContractFilter filter = new OpenApiContractFilter();

    @Test
    void givenNativeMetadata_whenReadByScanner_thenDeclaresPublishedServerAndBearerScheme() {
        OpenAPIDefinition definition = OpenApiApplication.class.getAnnotation(OpenAPIDefinition.class);
        org.eclipse.microprofile.openapi.annotations.security.SecurityScheme scheme = OpenApiApplication.class
                .getAnnotation(org.eclipse.microprofile.openapi.annotations.security.SecurityScheme.class);

        assertNotNull(definition);
        assertNotNull(scheme);
        assertEquals("${quarkus.smallrye-openapi.info-title}", definition.info().title());
        assertEquals("${quarkus.smallrye-openapi.info-version}", definition.info().version());
        assertEquals(1, definition.servers().length);
        assertEquals("http://localhost", definition.servers()[0].url());
        assertEquals("Generated server url", definition.servers()[0].description());
        assertEquals("bearerAuth", scheme.securitySchemeName());
        assertEquals(SecuritySchemeType.HTTP, scheme.type());
        assertEquals("bearer", scheme.scheme());
        assertEquals("JWT", scheme.bearerFormat());
        assertEquals("${openapi.security.schema.bearer.description}", scheme.description());
    }

    @Test
    void givenConfiguredDescription_whenSchemeFiltered_thenUsesOverride() {
        Config config = mock(Config.class);
        when(config.getOptionalValue("openapi.security.schema.bearer.description", String.class))
                .thenReturn(Optional.of("Configured bearer description"));
        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .description("${openapi.security.schema.bearer.description}");

        try (MockedStatic<ConfigProvider> provider = mockStatic(ConfigProvider.class)) {
            provider.when(ConfigProvider::getConfig).thenReturn(config);

            SecurityScheme result = filter.filterSecurityScheme(scheme);

            assertSame(scheme, result);
            assertEquals("Configured bearer description", result.getDescription());
        }
    }

    @Test
    void givenAbsentConfiguredDescription_whenSchemeFiltered_thenOmitsDescription() {
        Config config = mock(Config.class);
        when(config.getOptionalValue("openapi.security.schema.bearer.description", String.class))
                .thenReturn(Optional.empty());
        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .description("${openapi.security.schema.bearer.description}");

        try (MockedStatic<ConfigProvider> provider = mockStatic(ConfigProvider.class)) {
            provider.when(ConfigProvider::getConfig).thenReturn(config);

            SecurityScheme result = filter.filterSecurityScheme(scheme);

            assertSame(scheme, result);
            assertNull(result.getDescription());
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Literal bearer description"})
    void givenLiteralOrAbsentDescription_whenSchemeFiltered_thenPreservesDescription(String description) {
        SecurityScheme scheme = OASFactory.createSecurityScheme().description(description);

        SecurityScheme result = filter.filterSecurityScheme(scheme);

        assertSame(scheme, result);
        assertEquals(description, result.getDescription());
    }

    @Test
    void givenNativeServerAndScheme_whenCompatibilityFilterRuns_thenDoesNotRewriteMetadata() {
        Config config = mock(Config.class);
        when(config.getOptionalValue(anyString(), eq(String.class))).thenReturn(Optional.empty());
        Server server = OASFactory.createServer().url("http://localhost").description("Generated server url");
        SecurityScheme scheme = OASFactory.createSecurityScheme().type(SecurityScheme.Type.HTTP)
                .scheme("bearer").bearerFormat("JWT").description("Configured bearer description");
        OpenAPI openApi = OASFactory.createOpenAPI().servers(List.of(server))
                .components(OASFactory.createComponents().addSecurityScheme("bearerAuth", scheme));

        try (MockedStatic<ConfigProvider> provider = mockStatic(ConfigProvider.class)) {
            provider.when(ConfigProvider::getConfig).thenReturn(config);

            filter.filterOpenAPI(openApi);

            assertSame(server, openApi.getServers().get(0));
            assertSame(scheme, openApi.getComponents().getSecuritySchemes().get("bearerAuth"));
            assertNotNull(openApi.getComponents().getSchemas().get("Problem"));
            assertNull(openApi.getSecurity());
        }
    }

    @Test
    void givenConfiguredInfo_whenCompatibilityFilterRuns_thenResolvesTitleAndVersion() {
        Config config = mock(Config.class);
        when(config.getOptionalValue(anyString(), eq(String.class))).thenReturn(Optional.empty());
        when(config.getOptionalValue("quarkus.smallrye-openapi.info-title", String.class))
                .thenReturn(Optional.of("Configured API title"));
        when(config.getOptionalValue("quarkus.smallrye-openapi.info-version", String.class))
                .thenReturn(Optional.of("1.2.3"));
        OpenAPI openApi = OASFactory.createOpenAPI().info(OASFactory.createInfo()
                .title("${quarkus.smallrye-openapi.info-title}")
                .version("${quarkus.smallrye-openapi.info-version}")
                .description("Configured API description"));

        try (MockedStatic<ConfigProvider> provider = mockStatic(ConfigProvider.class)) {
            provider.when(ConfigProvider::getConfig).thenReturn(config);

            filter.filterOpenAPI(openApi);

            assertEquals("Configured API title", openApi.getInfo().getTitle());
            assertEquals("1.2.3", openApi.getInfo().getVersion());
            assertEquals("Configured API description", openApi.getInfo().getDescription());
        }
    }

    @Test
    void givenNoComponents_whenCompatibilityFilterRuns_thenStillRegistersProblemSchema() {
        Config config = mock(Config.class);
        when(config.getOptionalValue(anyString(), eq(String.class))).thenReturn(Optional.empty());
        OpenAPI openApi = OASFactory.createOpenAPI();

        try (MockedStatic<ConfigProvider> provider = mockStatic(ConfigProvider.class)) {
            provider.when(ConfigProvider::getConfig).thenReturn(config);

            filter.filterOpenAPI(openApi);

            assertNotNull(openApi.getComponents().getSchemas().get("Problem"));
        }
    }
}
