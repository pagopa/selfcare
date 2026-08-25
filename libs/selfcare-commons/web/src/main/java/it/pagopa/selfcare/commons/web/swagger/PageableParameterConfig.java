package it.pagopa.selfcare.commons.web.swagger;

import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

@Configuration
public class PageableParameterConfig {

  private final SpringDataWebProperties springDataWebProperties;

  public PageableParameterConfig(SpringDataWebProperties springDataWebProperties) {
    this.springDataWebProperties = springDataWebProperties;
  }

  @Bean(name = "customPageableCustomizer")
  public OperationCustomizer pageableCustomizer() {
    return (operation, handlerMethod) -> {
      if (handlerMethod.getMethodParameters() != null) {
        boolean hasPageable = List.of(handlerMethod.getMethodParameters()).stream()
          .anyMatch(param -> param.getParameter().getType().equals(Pageable.class));

        if (hasPageable) {
          operation.addParametersItem(createParameter(
            springDataWebProperties.getPageable().getPageParameter(),
            "The page number to access",
            "integer",
            "query"
          ));
          operation.addParametersItem(createParameter(
            springDataWebProperties.getPageable().getSizeParameter(),
            "Number of records per page",
            "integer",
            "query"
          ));
          operation.addParametersItem(createParameter(
            springDataWebProperties.getSort().getSortParameter(),
            "Sorting criteria in the format: property(,asc|desc). Default sort order is ascending. Multiple sort criteria are supported.",
            "string",
            "query"
          ));
        }
      }
      return operation;
    };
  }

  private Parameter createParameter(String name, String description, String schemaType, String in) {
    return new Parameter()
      .name(name)
      .description(description)
      .in(in)
      .schema(new io.swagger.v3.oas.models.media.Schema<>().type(schemaType));
  }
}
