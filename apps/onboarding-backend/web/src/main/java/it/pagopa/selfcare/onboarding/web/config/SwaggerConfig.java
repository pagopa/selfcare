package it.pagopa.selfcare.onboarding.web.config;

import com.fasterxml.classmate.TypeResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Objects;

/** The Class SwaggerConfig. */
@Configuration
class SwaggerConfig {

  private static final String AUTH_SCHEMA_NAME = "bearerAuth";

  @Configuration
  @Profile("swaggerIT")
  @PropertySource("classpath:/swagger/swagger_it.properties")
  public static class itConfig {}

  @Configuration
  @Profile("swaggerEN")
  @PropertySource("classpath:/swagger/swagger_en.properties")
  public static class enConfig {}

  private final Environment environment;

  @Bean
  public TypeResolver typeResolver() {
    return new TypeResolver();
  }

  @Autowired
  SwaggerConfig(Environment environment) {
    this.environment = environment;
  }

  @Bean
  @Primary
  public OpenAPI swagger() {
    return new OpenAPI()
        .info(
            new Info()
                .title(
                    environment.getProperty(
                        "swagger.title",
                        Objects.requireNonNull(environment.getProperty("spring.application.name"))))
                .description(environment.getProperty("swagger.description", "Api and Models"))
                .version(
                    environment.getProperty(
                        "swagger.version",
                        Objects.requireNonNull(
                            environment.getProperty("spring.application.version"))))
                .contact(new Contact().name("PagoPA").url("https://www.pagopa.gov.it")))
        .tags(
            List.of(
                new Tag()
                    .name("institutions")
                    .description(
                        environment.getProperty("swagger.onboarding.institutions.api.description")),
                new Tag()
                    .name("product")
                    .description(
                        environment.getProperty("swagger.onboarding.product.api.description")),
                new Tag()
                    .name("user")
                    .description(
                        environment.getProperty("swagger.onboarding.user.api.description"))))
        .components(
            new Components()
                .addSecuritySchemes(
                    AUTH_SCHEMA_NAME,
                    new SecurityScheme()
                        .name(AUTH_SCHEMA_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description(
                            "A bearer token in the format of a JWS and conformed to the specifications included in [RFC8725](https://tools.ietf.org/html/RFC8725)")
                        .bearerFormat("JWT")));
  }

  @Bean
  public OpenApiCustomizer globalResponsesCustomizer() {
    return openApi ->
        openApi
            .getPaths()
            .forEach(
                (path, pathItem) ->
                    pathItem
                        .readOperations()
                        .forEach(
                            operation -> {
                              operation.addSecurityItem(
                                  new SecurityRequirement()
                                      .addList(AUTH_SCHEMA_NAME, List.of("global")));
                              operation
                                  .getResponses()
                                  .addApiResponse(
                                      "400",
                                      new io.swagger.v3.oas.models.responses.ApiResponse()
                                          .description("Bad Request"));
                              operation
                                  .getResponses()
                                  .addApiResponse(
                                      "401",
                                      new io.swagger.v3.oas.models.responses.ApiResponse()
                                          .description("Unauthorized"));
                              operation
                                  .getResponses()
                                  .addApiResponse(
                                      "404",
                                      new io.swagger.v3.oas.models.responses.ApiResponse()
                                          .description("Not Found"));
                              operation
                                  .getResponses()
                                  .addApiResponse(
                                      "500",
                                      new io.swagger.v3.oas.models.responses.ApiResponse()
                                          .description("Internal Server Error"));
                            }));
  }
}
