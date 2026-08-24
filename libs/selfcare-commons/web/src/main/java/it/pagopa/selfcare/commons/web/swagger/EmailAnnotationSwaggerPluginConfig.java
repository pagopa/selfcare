package it.pagopa.selfcare.commons.web.swagger;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import io.swagger.v3.oas.models.media.StringSchema;
import jakarta.validation.constraints.Email;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailAnnotationSwaggerPluginConfig {

  @Bean
  public JacksonAnnotationIntrospector emailAnnotationIntrospector() {
    return new JacksonAnnotationIntrospector() {
      @Override
      public Object findSerializer(Annotated annotated) {
        if (annotated.hasAnnotation(Email.class)) {
          return new StringSchema()
            .format("email")
            .example("email@example.com");
        }
        return super.findSerializer(annotated);
      }
    };
  }
}
