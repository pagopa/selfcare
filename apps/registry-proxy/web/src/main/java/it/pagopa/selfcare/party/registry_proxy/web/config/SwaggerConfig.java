package it.pagopa.selfcare.party.registry_proxy.web.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.servers.ServerVariable;
import io.swagger.v3.oas.models.servers.ServerVariables;
import it.pagopa.selfcare.commons.web.swagger.BaseSwaggerConfig;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Configurazione OpenAPI (ex SwaggerConfig)
 */
@Slf4j
@Configuration
@Import(BaseSwaggerConfig.class)
@SecurityScheme(
    name = SwaggerConfig.AUTH_SCHEMA_NAME,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class SwaggerConfig {

    public static final String AUTH_SCHEMA_NAME = "bearerAuth";

    private final Environment environment;

    @Configuration
    @Profile("swaggerEN")
    @PropertySource("classpath:/swagger/swagger_en.properties")
    public static class enConfig {}

    public SwaggerConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Primary
    public OpenAPI swagger() {
        return new OpenAPI()
            .info(
                new Info()
                    .title(environment.getProperty("swagger.title", "API Documentation"))
                    .description(environment.getProperty("swagger.description", "API and Models"))
                    .version(environment.getProperty("swagger.version", "0.1.0-SNAPSHOT"))
                    .contact(new Contact().name("PagoPA").url("https://www.pagopa.gov.it")))
            .components(
                new Components()
                    .addSecuritySchemes(
                        AUTH_SCHEMA_NAME,
                        new io.swagger.v3.oas.models.security.SecurityScheme()
                            .name(AUTH_SCHEMA_NAME)
                            .type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .description(
                                "A bearer token in the format of a JWS and conformed to the specifications included in [RFC8725](https://tools.ietf.org/html/RFC8725)")
                            .bearerFormat("JWT")));
    }

    @Bean
    public OpenApiCustomizer globalResponsesCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
            pathItem.readOperations().forEach(operation -> {

                operation.addSecurityItem(
                    new SecurityRequirement()
                        .addList(AUTH_SCHEMA_NAME, List.of("global")));

                operation.getResponses().addApiResponse("400",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Bad Request"));
                operation.getResponses().addApiResponse("401",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Unauthorized"));
                operation.getResponses().addApiResponse("404",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Not Found"));
                operation.getResponses().addApiResponse("500",
                    new io.swagger.v3.oas.models.responses.ApiResponse()
                        .description("Internal Server Error"));
            }));
    }
}
