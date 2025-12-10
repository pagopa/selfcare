package it.pagopa.selfcare.product.mapper;

import it.pagopa.selfcare.product.conf.JacksonConfiguration;
import it.pagopa.selfcare.product.entity.BackOfficeConfigurations;
import it.pagopa.selfcare.product.model.BackOfficeEnvironmentConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

  private final ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

  private static JacksonConfiguration jacksonConfiguration;

  @BeforeAll
  static void setup(){
      jacksonConfiguration = new JacksonConfiguration();
  }

  @Test
  void mapBackOfficeConfigs_shouldMapEntriesByEnvironment() {
    BackOfficeEnvironmentConfiguration config = new BackOfficeEnvironmentConfiguration();
    config.setEnv("PROD");

    Map<String, BackOfficeConfigurations> result = mapper.mapBackOfficeConfigs(List.of(config));

    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.containsKey("PROD"));
    assertNotNull(result.get("PROD"));
  }

  @Test
  void mapContracts_shouldConvertModel() throws IOException {
      try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("product.json")) {
          assertNotNull(inputStream, "File product.json not found  in src/test/resources");

          it.pagopa.selfcare.product.model.Product product =
                  jacksonConfiguration.objectMapper().readValue(inputStream, it.pagopa.selfcare.product.model.Product.class);

          assertNotNull(product);

          it.pagopa.selfcare.product.entity.Product productEntity = mapper.toResource(product);

          assertNotNull(productEntity);
          assertEquals(product.getProductId(), productEntity.getId());
          assertEquals(product.getDescription(), productEntity.getDescription());
          String jsonEntity = jacksonConfiguration.objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(productEntity);
          System.out.println("Mapped Product Entity:");
          System.out.println(jsonEntity);
      }
  }
}