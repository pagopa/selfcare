package it.pagopa.selfcare.dashboard.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import io.swagger.v3.oas.models.tags.Tag;
import it.pagopa.selfcare.commons.web.model.Problem;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Class SwaggerConfig.
 */
@Configuration
public class SwaggerConfig {

    private static final String AUTH_SCHEMA_NAME = "bearerAuth";

    @Configuration
    @Profile("swaggerIT")
    @PropertySource("classpath:/swagger/swagger_it.properties")
    public static class itConfig {
    }

    @Configuration
    @Profile("swaggerEN")
    @PropertySource("classpath:/swagger/swagger_en.properties")
    public static class enConfig {
    }

    private final Environment environment;


    @Autowired
    SwaggerConfig(Environment environment) {
        this.environment = environment;
    }


    @Bean
    @Primary
    public OpenAPI swaggerSpringPlugin() {
        return (new OpenAPI(SpecVersion.V30))
                .info(new Info()
                        .title("${openapi_title}")
                        .description(environment.getProperty("swagger.description", "Api and Models"))
                        .version(environment.getProperty("swagger.version", environment.getProperty("spring.application.version")))
                )
                .servers(List.of(
                        new Server().url("{url}:{port}{basePath}").variables(new ServerVariables()
                                .addServerVariable("url", new ServerVariable()._default("http://localhost"))
                                .addServerVariable("port", new ServerVariable()._default("80"))
                                .addServerVariable("basePath", new ServerVariable()._default(""))
                        )
                ))
                .tags(List.of(
                        new Tag().name("delegations").description(environment.getProperty("swagger.dashboard.delegations.api.description")),
                        new Tag().name("institutions").description(environment.getProperty("swagger.dashboard.institutions.api.description")),
                        new Tag().name("onboarding").description(environment.getProperty("swagger.dashboard.onboarding.api.description")),
                        new Tag().name("pnPGInstitutions").description(environment.getProperty("swagger.dashboard.pnPGInstitutions.api.description")),
                        new Tag().name("products").description(environment.getProperty("swagger.dashboard.product.api.description")),
                        new Tag().name("relationships").description(environment.getProperty("swagger.dashboard.relationships.api.description")),
                        new Tag().name("support").description(environment.getProperty("swagger.dashboard.support.api.description")),
                        new Tag().name("token").description(environment.getProperty("swagger.dashboard.token.api.description")),
                        new Tag().name("user").description(environment.getProperty("swagger.dashboard.user.api.description")),
                        new Tag().name("user-groups").description(environment.getProperty("swagger.dashboard.user-groups.api.description"))
                ))
                .components(new Components()
                        .addSecuritySchemes(
                                AUTH_SCHEMA_NAME,
                                new SecurityScheme()
                                        .name(AUTH_SCHEMA_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description(environment.getProperty("swagger.security.schema.bearer.description"))
                        )
                );
    }

    @Bean
    public GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder()
                .group("dashboard")
                .packagesToScan("it.pagopa.selfcare.dashboard.controller")
                .build();
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        final Map<String, Schema> problemComponent = ModelConverters.getInstance().read(Problem.class);
        final Map<String, Schema> invalidParamComponent = ModelConverters.getInstance().read(Problem.InvalidParam.class);
        final Schema<?> problemSchema = new Schema<>().$ref("#/components/schemas/Problem").jsonSchemaImpl(Problem.class);
        final Content problemContent = new Content().addMediaType("application/problem+json", new MediaType().schema(problemSchema));
        return openApi -> {
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                        final ApiResponses responses = operation.getResponses();

                        // Remove 404 for non-GET methods
                        if (!httpMethod.name().equalsIgnoreCase("GET")) {
                            responses.remove("404");
                        }

                        // Handle forced 404 response
                        if (responses.containsKey("#404")) {
                            final ApiResponse forced404Response = responses.get("#404");
                            responses.remove("#404");
                            responses.addApiResponse("404", forced404Response);
                        }

                        // Include HTTP method in operationId (if it doesn't start with # and not already present)
                        Optional.ofNullable(operation.getOperationId()).ifPresent(opid -> {
                            if (opid.startsWith("#")) {
                                operation.setOperationId(opid.replace("#", ""));
                            } else if (!opid.endsWith("Using" + httpMethod.name())) {
                                operation.setOperationId(opid + "Using" + httpMethod.name());
                            }
                        });

                        // Set parameter descriptions to parameter names if missing and configure array query parameters
                        Optional.ofNullable(operation.getParameters()).ifPresent(params -> {
                            params.forEach(p -> {
                                if (p.getDescription() == null) {
                                    p.setDescription(p.getName());
                                }

                                // Default springdoc style: param=element1&parma=element2
                                // FE expects springfox style: param=element1,element2
                                if (p.getIn() != null && p.getIn().equals("query")) {
                                    if (p.getSchema() instanceof ArraySchema && p.getSchema().getItems() != null) {
                                        if ("array".equals(p.getSchema().getType()) && "string".equals(p.getSchema().getItems().getType())) {
                                            p.setStyle(Parameter.StyleEnum.FORM);
                                            p.setExplode(true);
                                            p.setSchema(new StringSchema());
                                        }
                                    }
                                }
                            });
                        });

                        // Standard error responses
                        Optional.ofNullable(responses.get("400"))
                                .ifPresent(r -> r.description("Bad Request").content(problemContent));
                        Optional.ofNullable(responses.get("401"))
                                .ifPresent(r -> r.description("Unauthorized").content(problemContent));
                        Optional.ofNullable(responses.get("404"))
                                .ifPresent(r -> r.description("Not Found").content(problemContent));
                        Optional.ofNullable(responses.get("409"))
                                .ifPresent(r -> r.description("Conflict").content(problemContent));
                        Optional.ofNullable(responses.get("500"))
                                .ifPresent(r -> r.description("Internal Server Error").content(problemContent));

                        // Security requirement
                        operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth", List.of("global")));
                    })
            );

            // Add Problem to components
            openApi.getComponents().addSchemas("Problem", problemComponent.get("Problem"));
            openApi.getComponents().addSchemas("InvalidParam", invalidParamComponent.get("InvalidParam"));

            // Sort components alphabetically
            //openApi.getComponents().setSchemas(new TreeMap<>(openApi.getComponents().getSchemas()));

            // Resolve description placeholders in schemas
            openApi.getComponents().getSchemas().values().forEach(c -> {
                resolveSchemaDescriptionPlaceholder(c);
                if (c.getProperties() != null) {
                    final Map<String, Schema<?>> properties = c.getProperties();
                    properties.forEach((k, v) -> resolveSchemaDescriptionPlaceholder(v));
                }
            });
        };
    }

    private void resolveSchemaDescriptionPlaceholder(Schema<?> s) {
        if (s.getDescription() != null && s.getDescription().startsWith("${")) {
            s.setDescription(environment.resolvePlaceholders(s.getDescription()));
        }

        if (s.getItems() != null) {
            resolveSchemaDescriptionPlaceholder(s.getItems());
        }
    }

}
