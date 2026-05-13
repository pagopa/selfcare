package it.pagopa.selfcare.external_api.config;

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
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Class SwaggerConfig.
 */
@Configuration
class SwaggerConfig {

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

    // Remove the globalApiCustomizer that comes from commons
    // BaseSwaggerConfig -> globalApiCustomizer is component scanned by ExternalAPISecurityConfig -> SecurityConfig
    @Bean
    public static BeanDefinitionRegistryPostProcessor removeGlobalApiCustomizer() {
        return new BeanDefinitionRegistryPostProcessor() {

            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition("globalApiCustomizer")) {
                    registry.removeBeanDefinition("globalApiCustomizer");
                }
            }

            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
            }
        };
    }

    @Bean
    @Primary
    public OpenAPI swaggerSpringPlugin() {
        return (new OpenAPI(SpecVersion.V30))
                .info(new Info()
                        .title(environment.getProperty("swagger.title", environment.getProperty("spring.application.name")))
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
                        new Tag().name("Institution").description("Institution V 2 Controller"),
                        new Tag().name("Onboarding").description("Onboarding V 2 Controller"),
                        new Tag().name("Proxy").description("National Registry Controller"),
                        new Tag().name("Token").description("Token Controller"),
                        new Tag().name("User").description("User V 2 Controller"),
                        new Tag().name("institutions-pnpg").description("Pn Pg Controller")
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
    public GroupedOpenApi externalApi() {
        return GroupedOpenApi.builder()
                .group("external_api")
                .packagesToScan("it.pagopa.selfcare.external_api.controller")
                .build();
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer() {
        final Map<String, Schema> problemComponent = ModelConverters.getInstance().read(Problem.class);
        final Map<String, Schema> invalidParamComponent = ModelConverters.getInstance().read(Problem.InvalidParam.class);
        final Schema<?> problemSchema = new Schema<>().$ref("#/components/schemas/Problem").jsonSchemaImpl(Problem.class);
        final Content problemContent = new Content().addMediaType("application/problem+json", new MediaType().schema(problemSchema));
        return openApi -> {
            // Customize Paths
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
                        operation.getResponses().addApiResponse("400", new ApiResponse());
                        operation.getResponses().addApiResponse("401", new ApiResponse());
                        if (httpMethod.name().equalsIgnoreCase("GET") ||
                            httpMethod.name().equalsIgnoreCase("HEAD")) {
                            operation.getResponses().addApiResponse("404", new ApiResponse());
                        }
                        operation.getResponses().addApiResponse("500", new ApiResponse());

                        // Sort responses by status code
                        operation.setResponses(operation.getResponses().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .collect(
                                        ApiResponses::new,
                                        (r, e) -> r.put(e.getKey(), e.getValue()),
                                        ApiResponses::putAll
                                ));

                        // Include HTTP method in operationId (if it doesn't start with # and not already present)
                        Optional.ofNullable(operation.getOperationId()).ifPresent(opid -> {
                            if (opid.startsWith("#")) {
                                operation.setOperationId(opid.replace("#", ""));
                            } else if (!opid.endsWith("Using" + httpMethod.name())) {
                                operation.setOperationId(opid + "Using" + httpMethod.name());
                            }
                        });

                        // Set parameter descriptions to parameter names if missing and configure array query parameters and style
                        Optional.ofNullable(operation.getParameters()).ifPresent(params -> {
                            params.forEach(p -> {
                                if (p.getDescription() == null) {
                                    p.setDescription(p.getName());
                                }

                                // Default springdoc style: param=element1&parma=element2
                                // Using springfox style: param=element1,element2
                                if (p.getStyle() == null && p.getIn() != null && p.getIn().equals("query")) {
                                    p.setStyle(Parameter.StyleEnum.FORM);
                                    if (p.getSchema() instanceof ArraySchema && p.getSchema().getItems() != null) {
                                        if ("array".equals(p.getSchema().getType()) && "string".equals(p.getSchema().getItems().getType())) {
                                            p.setExplode(true);
                                            final StringSchema schema = new StringSchema();
                                            schema.setEnum(p.getSchema().getItems().getEnum());
                                            p.setSchema(schema);
                                        }
                                    }
                                }

                                if (p.getStyle() == null && p.getIn() != null && p.getIn().equals("path")) {
                                    p.setStyle(Parameter.StyleEnum.SIMPLE);
                                }

                            });
                        });

                        // Standard error responses
                        Optional.ofNullable(operation.getResponses().get("400"))
                                .ifPresent(r -> r.description("Bad Request").content(problemContent));
                        Optional.ofNullable(operation.getResponses().get("401"))
                                .ifPresent(r -> r.description("Unauthorized").content(problemContent));
                        Optional.ofNullable(operation.getResponses().get("403"))
                                .ifPresent(r -> r.description("Forbidden").content(problemContent));
                        Optional.ofNullable(operation.getResponses().get("404"))
                                .ifPresent(r -> r.description("Not Found").content(problemContent));
                        Optional.ofNullable(operation.getResponses().get("409"))
                                .ifPresent(r -> r.description("Conflict").content(problemContent));
                        Optional.ofNullable(operation.getResponses().get("500"))
                                .ifPresent(r -> r.description("Internal Server Error").content(problemContent));

                        // Sort tags
                        //Optional.ofNullable(operation.getTags())
                        //        .ifPresent(Collections::sort);

                        // Remove required flag from request body to align with spring boot 2 generated openapi
                        Optional.ofNullable(operation.getRequestBody())
                                .ifPresent(rb -> rb.setRequired(null));

                        // Security requirement
                        operation.addSecurityItem(new SecurityRequirement().addList("bearerAuth", List.of("global")));
                    })
            );

            // Add Problem to components
            openApi.getComponents().addSchemas("Problem", problemComponent.get("Problem"));
            openApi.getComponents().addSchemas("InvalidParam", invalidParamComponent.get("InvalidParam"));

            // Sort paths alphabetically
            //Map<String, PathItem> sortedPaths = new TreeMap<>(openApi.getPaths());
            //openApi.setPaths(new Paths());
            //openApi.getPaths().putAll(sortedPaths);

            // Sort components alphabetically
            //openApi.getComponents().setSchemas(new TreeMap<>(openApi.getComponents().getSchemas()));

            // Customize Components
            openApi.getComponents().getSchemas().values().forEach(c -> {
                // Set title
                c.setTitle(c.getName());
                // Resolve description placeholders in schemas
                resolveSchemaDescriptionPlaceholder(c);
                if (c.getProperties() != null) {
                    final Map<String, Schema<?>> properties = c.getProperties();
                    // Resolve description placeholders in schemas
                    properties.forEach((k, v) -> resolveSchemaDescriptionPlaceholder(v));
                    // Sort properties alphabetically
                    //c.setProperties(new TreeMap<>(properties));
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
