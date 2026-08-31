package it.pagopa.selfcare.commons.web.swagger;


import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.media.Content;
import org.springframework.http.HttpStatus;

@EnableConfigurationProperties(SpringDataWebProperties.class)
@Configuration
public class BaseSwaggerConfig {

  @Bean
  public GlobalOpenApiCustomizer globalApiCustomizer() {
    return openApi -> {
      // Definizione del modello "Problem"
      Schema<?> problemSchema = new Schema<>().type("object").description("Generic problem response");

      // Creazione delle risposte standard
      ApiResponse badRequest = createApiResponse(HttpStatus.BAD_REQUEST, problemSchema);
      ApiResponse unauthorized = createApiResponse(HttpStatus.UNAUTHORIZED, problemSchema);
      ApiResponse notFound = createApiResponse(HttpStatus.NOT_FOUND, problemSchema);
      ApiResponse internalError = createApiResponse(HttpStatus.INTERNAL_SERVER_ERROR, problemSchema);

      // Aggiunta dello schema "Problem" ai componenti globali di OpenAPI
      openApi.getComponents().addSchemas("Problem", problemSchema);

      // Aggiunta delle risposte globali a tutte le API
      openApi.getPaths().values().forEach(pathItem ->
        pathItem.readOperations().forEach(operation -> {
          ApiResponses responses = operation.getResponses();
          responses.addApiResponse("400", badRequest);
          responses.addApiResponse("401", unauthorized);
          responses.addApiResponse("404", notFound);
          responses.addApiResponse("500", internalError);
        }));
    };
  }

  private ApiResponse createApiResponse(HttpStatus status, Schema<?> schema) {
    return new ApiResponse()
      .description(status.getReasonPhrase())
      .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE,
        new MediaType().schema(schema)));
  }
}
