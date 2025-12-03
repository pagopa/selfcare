package it.pagopa.selfcare.product.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.selfcare.product.entity.BackOfficeConfigurations;
import it.pagopa.selfcare.product.model.BackOfficeEnvironmentConfiguration;
import it.pagopa.selfcare.product.model.ContractTemplate;
import it.pagopa.selfcare.product.model.enums.OnboardingType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductMapperTest {

  private final ProductMapper mapper = Mappers.getMapper(ProductMapper.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

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
  void mapContracts_shouldIncludeOnlyMatchingOnboardingType() {
    ContractTemplate institutionTemplate = new ContractTemplate();
    institutionTemplate.setOnboardingType(OnboardingType.INSTITUTION);
    institutionTemplate.setPath("/contracts/inst.pdf");
    institutionTemplate.setVersion("v1");

    ContractTemplate userTemplate = new ContractTemplate();
    userTemplate.setOnboardingType(OnboardingType.USER);
    userTemplate.setPath("/contracts/user.pdf");
    userTemplate.setVersion("v1");

    Map<String, it.pagopa.selfcare.product.entity.ContractTemplate> result = mapper.mapContracts(
        List.of(institutionTemplate, userTemplate),
        OnboardingType.INSTITUTION
    );

    assertEquals(1, result.size());
    assertTrue(result.containsKey("default"));
    assertEquals("/contracts/inst.pdf", result.get("default").getContractTemplatePath());
  }

  @Test
  void mapContracts_shouldConvertModel() throws IOException {
    
      objectMapper.registerModule(new JavaTimeModule());
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

      try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("product.json")) {
          assertNotNull(inputStream, "File product.json not found  in src/test/resources");

          it.pagopa.selfcare.product.model.Product product =
              objectMapper.readValue(inputStream, it.pagopa.selfcare.product.model.Product.class);

          assertNotNull(product);

          it.pagopa.selfcare.product.entity.Product productEntity = mapper.toResource(product);

          assertNotNull(productEntity);
          assertEquals(product.getId(), productEntity.getId());
          assertEquals(product.getDescription(), productEntity.getDescription());
          String jsonEntity = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(productEntity);
          System.out.println("Mapped Product Entity:");
          System.out.println(jsonEntity);
      }
  }
}