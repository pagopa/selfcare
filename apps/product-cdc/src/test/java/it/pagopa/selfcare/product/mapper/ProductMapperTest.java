package it.pagopa.selfcare.product.mapper;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.product.conf.JacksonConfiguration;
import it.pagopa.selfcare.product.entity.BackOfficeConfigurations;
import it.pagopa.selfcare.product.model.BackOfficeEnvironmentConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ProductMapperTest {

  private final ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

  private static JacksonConfiguration jacksonConfiguration;

  @BeforeAll
  static void setup() {
    jacksonConfiguration = new JacksonConfiguration();
  }

  @Test
  void mapBackOfficeConfigs_shouldMapEntriesByEnvironment() {
    BackOfficeEnvironmentConfiguration config = new BackOfficeEnvironmentConfiguration();
    config.setEnv("PROD");

    Map<String, BackOfficeConfigurations> result = mapper.mapBackOfficeConfigs(List.of(config));

    assertNotNull(result);
    assertEquals(0, result.size());
  }

  @Test
  void mapContracts_shouldConvertModel() throws IOException {
    // given
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("product.json")) {
      assertNotNull(inputStream, "File product.json not found  in src/test/resources");

      it.pagopa.selfcare.product.model.Product product =
          jacksonConfiguration
              .objectMapper()
              .readValue(inputStream, it.pagopa.selfcare.product.model.Product.class);

      assertNotNull(product);

      // when
      it.pagopa.selfcare.product.entity.Product productEntity = mapper.toResource(product);

      // then
      assertNotNull(productEntity);
      assertEquals(product.getProductId(), productEntity.getId());
      assertEquals(product.getParentId(), productEntity.getParentId());
      assertEquals("https://baseurl.it/", productEntity.getUrlPublic());
      assertEquals(
          "https://baseurl.it/idp/selfcare/resolve-identity?id=<IdentityToken>",
          productEntity.getUrlBO());
      assertNotNull(productEntity.getBackOfficeEnvironmentConfigurations());
      assertFalse(productEntity.getBackOfficeEnvironmentConfigurations().containsKey("PROD"));
      assertTrue(productEntity.getBackOfficeEnvironmentConfigurations().containsKey("Locale"));
      assertEquals(
          product.getInstitutionTypesAllowed(), productEntity.getInstitutionTypesAllowed());
      assertEquals(
          product.getFeatures().isRequiresParentOnboarding(),
          productEntity.isRequiresParentOnboarding());
      assertEquals(product.getDescription(), productEntity.getDescription());
      assertEquals("identity.it", productEntity.getIdentityTokenAudience());
      String jsonEntity =
          jacksonConfiguration
              .objectMapper()
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(productEntity);
      System.out.println("Mapped Product Entity:");
      System.out.println(jsonEntity);
    }
  }

  @Test
  void toResource_shouldPropagateDedicatedDataIsolationToTheProductJson() {
    // given: product-cdc is the only hop between the Mongo product and the JSON consumers read to
    // route their persistence, so a dropped field here would silently send every product back to
    // the shared database.
    it.pagopa.selfcare.product.model.Product product =
        new it.pagopa.selfcare.product.model.Product();
    product.setProductId("prod-dedicated");
    product.setDataIsolation(
        it.pagopa.selfcare.product.model.DataIsolationConfig.builder()
            .database(it.pagopa.selfcare.product.model.enums.DatabaseIsolationModel.DEDICATED)
            .databaseName("selcOnboardingDedicated")
            .build());

    // when
    it.pagopa.selfcare.product.entity.Product entity = mapper.toResource(product);

    // then
    assertNotNull(entity.getDataIsolation());
    assertEquals(
        it.pagopa.selfcare.product.entity.DatabaseIsolationModel.DEDICATED,
        entity.getDataIsolation().getDatabase());
    assertEquals("selcOnboardingDedicated", entity.getDataIsolation().getDatabaseName());
    assertTrue(entity.getDataIsolation().isDedicatedDatabase());
    assertEquals(
        it.pagopa.selfcare.product.entity.DatabaseIsolationModel.DEDICATED,
        entity.resolveDatabaseIsolationModel());
  }

  @Test
  void toResource_whenNoDataIsolation_shouldResolveAsShared() {
    it.pagopa.selfcare.product.model.Product product =
        new it.pagopa.selfcare.product.model.Product();
    product.setProductId("prod-legacy");

    it.pagopa.selfcare.product.entity.Product entity = mapper.toResource(product);

    assertNull(entity.getDataIsolation());
    assertEquals(
        it.pagopa.selfcare.product.entity.DatabaseIsolationModel.SHARED,
        entity.resolveDatabaseIsolationModel());
  }
}
