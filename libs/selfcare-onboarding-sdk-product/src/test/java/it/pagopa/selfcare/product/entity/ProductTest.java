package it.pagopa.selfcare.product.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ProductTest {

  private static final Logger log = LoggerFactory.getLogger(ProductTest.class);
  public static final String INSTITUTION_TYPE_DEFAULT = "DEFAULT";

  ProductRoleInfo dummmyProductRoleInfo(PartyRole partyRole) {
    ProductRoleInfo productRoleInfo = new ProductRoleInfo();
    ProductRole productRole = new ProductRole();
    productRole.setCode(partyRole.name());
    productRoleInfo.setRoles(List.of(productRole));
    return productRoleInfo;
  }

  @Test
  @DisplayName("Test when only roleMappings is non-null")
  public void testGetAllRoleMappings_OnlyRoleMappings() {
    Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
    ProductRoleInfo manager = dummmyProductRoleInfo(PartyRole.MANAGER);
    ProductRoleInfo operator = dummmyProductRoleInfo(PartyRole.OPERATOR);
    roleMappings.put(PartyRole.MANAGER, manager);
    roleMappings.put(PartyRole.OPERATOR, operator);

    Product product = new Product();
    product.setRoleMappings(roleMappings);
    product.setRoleMappingsByInstitutionType(null);

    Map<PartyRole, List<ProductRoleInfo>> result = product.getAllRoleMappings();

    assertEquals(2, result.size(), "Map should contain 2 keys");
    assertEquals(List.of(manager), result.get(PartyRole.MANAGER));
    assertEquals(List.of(operator), result.get(PartyRole.OPERATOR));
  }

  @Test
  @DisplayName("Test when only roleMappingsByInstitutionType is non-null")
  public void testGetAllRoleMappings_OnlyRoleMappingsByInstitutionType() {
    Map<String, Map<PartyRole, ProductRoleInfo>> roleMappingsByInstitutionType = new HashMap<>();

    Map<PartyRole, ProductRoleInfo> institution1 = new HashMap<>();
    institution1.put(PartyRole.MANAGER, dummmyProductRoleInfo(PartyRole.MANAGER));
    institution1.put(PartyRole.OPERATOR, dummmyProductRoleInfo(PartyRole.OPERATOR));

    Map<PartyRole, ProductRoleInfo> institution2 = new HashMap<>();
    institution2.put(PartyRole.DELEGATE, dummmyProductRoleInfo(PartyRole.DELEGATE));
    institution2.put(PartyRole.OPERATOR, dummmyProductRoleInfo(PartyRole.OPERATOR));

    roleMappingsByInstitutionType.put("Institution1", institution1);
    roleMappingsByInstitutionType.put("Institution2", institution2);

    Product product = new Product();
    product.setRoleMappings(null);
    product.setRoleMappingsByInstitutionType(roleMappingsByInstitutionType);

    Map<PartyRole, List<ProductRoleInfo>> result = product.getAllRoleMappings();

    assertEquals(3, result.size(), "Map should contain 3 keys");
    assertEquals(List.of(dummmyProductRoleInfo(PartyRole.MANAGER)), result.get(PartyRole.MANAGER));
    assertEquals(
            List.of(dummmyProductRoleInfo(PartyRole.DELEGATE)), result.get(PartyRole.DELEGATE));
    assertEquals(
            Arrays.asList(
                    dummmyProductRoleInfo(PartyRole.OPERATOR), dummmyProductRoleInfo(PartyRole.OPERATOR)),
            result.get(PartyRole.OPERATOR));
  }

  @Test
  @DisplayName("Test when both maps are non-null with overlapping keys")
  public void testGetAllRoleMappings_BothMapsNonNullWithOverlap() {
    // Setup roleMappings
    Map<PartyRole, ProductRoleInfo> roleMappings = new HashMap<>();
    roleMappings.put(PartyRole.MANAGER, dummmyProductRoleInfo(PartyRole.MANAGER));
    roleMappings.put(PartyRole.DELEGATE, dummmyProductRoleInfo(PartyRole.OPERATOR));

    // Setup roleMappingsByInstitutionType
    Map<String, Map<PartyRole, ProductRoleInfo>> roleMappingsByInstitutionType = new HashMap<>();

    Map<PartyRole, ProductRoleInfo> institution1 = new HashMap<>();
    institution1.put(PartyRole.MANAGER, dummmyProductRoleInfo(PartyRole.MANAGER));
    institution1.put(PartyRole.OPERATOR, dummmyProductRoleInfo(PartyRole.OPERATOR));

    Map<PartyRole, ProductRoleInfo> institution2 = new HashMap<>();
    institution2.put(PartyRole.DELEGATE, dummmyProductRoleInfo(PartyRole.DELEGATE));
    institution2.put(PartyRole.OPERATOR, dummmyProductRoleInfo(PartyRole.OPERATOR));

    roleMappingsByInstitutionType.put("Institution1", institution1);
    roleMappingsByInstitutionType.put("Institution2", institution2);

    Product product = new Product();
    product.setRoleMappings(roleMappings);
    product.setRoleMappingsByInstitutionType(roleMappingsByInstitutionType);

    Map<PartyRole, List<ProductRoleInfo>> result = product.getAllRoleMappings();

    assertEquals(3, result.size(), "Map should contain 3 keys");

    // Verify MANAGER
    List<ProductRoleInfo> adminList = result.get(PartyRole.MANAGER);
    assertNotNull(adminList, "List for MANAGER should not be null");
    assertEquals(2, adminList.size(), "List for MANAGER should contain 2 elements");
    assertTrue(adminList.contains(dummmyProductRoleInfo(PartyRole.MANAGER)));
    assertTrue(adminList.contains(dummmyProductRoleInfo(PartyRole.MANAGER)));

    // Verify DELEGATE
    List<ProductRoleInfo> userList = result.get(PartyRole.DELEGATE);
    assertNotNull(userList, "List for DELEGATE should not be null");
    assertEquals(2, userList.size(), "List for DELEGATE should contain 2 elements");
    assertTrue(userList.contains(dummmyProductRoleInfo(PartyRole.DELEGATE)));
    assertTrue(userList.contains(dummmyProductRoleInfo(PartyRole.DELEGATE)));

    // Verify OPERATOR
    List<ProductRoleInfo> guestList = result.get(PartyRole.OPERATOR);
    assertNotNull(guestList, "List for OPERATOR should not be null");
    assertEquals(2, guestList.size(), "List for OPERATOR should contain 2 elements");
    assertTrue(guestList.contains(dummmyProductRoleInfo(PartyRole.OPERATOR)));
    assertTrue(guestList.contains(dummmyProductRoleInfo(PartyRole.OPERATOR)));
  }

  @Test
  @DisplayName("Test for Institution when only institutionType is into the map")
  public void getUserContractMappingsByKeyTest() {

    // given
    InstitutionType institutionType = InstitutionType.PSP;

    ContractTemplate ContractTemplate = new ContractTemplate();
    ContractTemplate.setContractTemplatePath("test");
    ContractTemplate.setContractTemplateVersion("test-version");

    Map<String, ContractTemplate> mapTest = new HashMap<>();
    mapTest.put(institutionType.toString(), ContractTemplate);

    Product product = new Product();
    product.setUserContractMappings(mapTest);

    // when
    ContractTemplate result = product.getUserContractTemplate(institutionType.toString());

    // then
    assertNotNull(result);
    assertTrue(Objects.nonNull(result));
    assertTrue(StringUtils.isNotEmpty(result.getContractTemplatePath()));
    assertTrue(StringUtils.isNotEmpty(result.getContractTemplateVersion()));
  }

  @Test
  @DisplayName("Test for User when only institutionType is not into the map")
  public void getUserContractMappingsByKeyTest_KO() {

    // given
    InstitutionType institutionType = InstitutionType.PSP;

    ContractTemplate ContractTemplate = new ContractTemplate();
    ContractTemplate.setContractTemplatePath("test");
    ContractTemplate.setContractTemplateVersion("test-version");

    Map<String, ContractTemplate> mapTest = new HashMap<>();
    mapTest.put(institutionType.toString(), ContractTemplate);
    mapTest.put(INSTITUTION_TYPE_DEFAULT, ContractTemplate);

    Product product = new Product();
    product.setUserContractMappings(mapTest);

    // when
    ContractTemplate result = product.getUserContractTemplate(InstitutionType.PRV.toString());

    // then
    assertNotNull(result);
    assertTrue(StringUtils.isNotEmpty(result.getContractTemplatePath()));
    assertTrue(StringUtils.isNotEmpty(result.getContractTemplateVersion()));
  }

  @Test
  @DisplayName("Test for Institution when only institutionType is into the map")
  public void getInstitutionContractMappingsByKeyTest() {

    // given
    InstitutionType institutionType = InstitutionType.PSP;

    ContractTemplate contractTemplate = new ContractTemplate();
    contractTemplate.setContractTemplatePath("test");
    contractTemplate.setContractTemplateVersion("test-version");

    Map<String, ContractTemplate> mapTest = new HashMap<>();
    mapTest.put(institutionType.toString(), contractTemplate);

    Product product = new Product();
    product.setInstitutionContractMappings(mapTest);

    // when
    ContractTemplate result = product.getInstitutionContractTemplate(institutionType.toString());

    // then
    assertNotNull(result);
    assertTrue(Objects.nonNull(result));
    assertTrue(StringUtils.isNotEmpty(result.getContractTemplatePath()));
    assertTrue(StringUtils.isNotEmpty(result.getContractTemplateVersion()));
  }

  @Test
  @DisplayName("Test for Institution when only institutionType is not into the map")
  public void getInstitutionContractMappingsByKeyTest_KO() {

    // given
    InstitutionType institutionType = InstitutionType.PSP;

    ContractTemplate contractTemplate = new ContractTemplate();
    contractTemplate.setContractTemplatePath("test");
    contractTemplate.setContractTemplateVersion("test-version");

    Map<String, ContractTemplate> mapTest = new HashMap<>();
    mapTest.put(institutionType.toString(), contractTemplate);
    mapTest.put(INSTITUTION_TYPE_DEFAULT, contractTemplate);

    Product product = new Product();
    product.setInstitutionContractMappings(mapTest);

    // when
    ContractTemplate result =
            product.getInstitutionContractTemplate(InstitutionType.PRV.toString());

    // then
    assertNotNull(result);
    assertTrue(StringUtils.isNotEmpty(result.getContractTemplatePath()));
    assertTrue(StringUtils.isNotEmpty(result.getContractTemplateVersion()));
  }

  @Test
  void productTest() {
    // given
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Product product = new Product();
    JsonNode jsonNode = null;

    // when
    try {
      product = objectMapper.readValue(new File("src/test/resources/product.json"), Product.class);

      FileInputStream fis = new FileInputStream("src/test/resources/product.json");
      String data = IOUtils.toString(fis, StandardCharsets.UTF_8);

      jsonNode = objectMapper.readTree(data);

    } catch (IOException e) {
      log.error("", e);
    }

    // then
    assertNotNull(jsonNode);
    assertEquals(product.getAlias(), jsonNode.get("alias").asText());
    assertEquals(product.getId(), jsonNode.get("id").asText());
    assertEquals(
            product.getInstitutionContractMappings().get(INSTITUTION_TYPE_DEFAULT).getContractTemplatePath(),
            jsonNode
                    .get("institutionContractMappings")
                    .get(INSTITUTION_TYPE_DEFAULT)
                    .get("contractTemplatePath")
                    .asText());
    assertEquals(product.getStatus().toString(), jsonNode.get("status").asText());
  }

  @Test
  void testGetUserAggregatorContractTemplate_WithValidInstitutionType() {
    // Setup
    Product product = new Product();
    Map<String, ContractTemplate> mappings = new HashMap<>();
    ContractTemplate contractTemplate = new ContractTemplate();
    contractTemplate.setContractTemplatePath("path");
    contractTemplate.setContractTemplatePath("version");
    mappings.put("validType", contractTemplate);
    product.setUserAggregatorContractMappings(mappings);

    // Execute
    ContractTemplate result = product.getUserAggregatorContractTemplate("validType");

    // Verify
    assertNotNull(result);
    assertEquals(mappings.get("validType"), result);
  }

  @Test
  void testGetUserAggregatorContractTemplate_WithDefault() {
    // Setup
    Product product = new Product();
    Map<String, ContractTemplate> mappings = new HashMap<>();
    ContractTemplate contractTemplate = new ContractTemplate();
    contractTemplate.setContractTemplatePath("path");
    contractTemplate.setContractTemplatePath("version");
    mappings.put(INSTITUTION_TYPE_DEFAULT, contractTemplate);
    product.setUserAggregatorContractMappings(mappings);

    // Execute
    ContractTemplate result = product.getUserAggregatorContractTemplate("unknownType");

    // Verify
    assertNotNull(result);
    assertEquals(mappings.get(INSTITUTION_TYPE_DEFAULT), result);
  }

  @Test
  void testGetUserAggregatorContractTemplate_NoMappings() {
    // Setup
    Product product = new Product();
    product.setUserAggregatorContractMappings(null);

    // Execute
    ContractTemplate result = product.getUserAggregatorContractTemplate("anyType");

    // Verify
    assertNotNull(result);
    assertEquals(ContractTemplate.class, result.getClass());
    assertNull(result.getContractTemplatePath());
    assertNull(result.getContractTemplateVersion());
  }

  @Test
  void testGetEmailTemplate_WithValidInstitutionAndWorkflowType() {
    // Setup
    final var status = OnboardingStatus.PENDING;
    Product product = new Product();
    Map<String, Map<String, List<EmailTemplate>>> mappings = new HashMap<>();
    EmailTemplate emailTemplate = new EmailTemplate();
    emailTemplate.setPath("path");
    emailTemplate.setStatus(status);
    mappings.put("validType", Map.of("workflowType", List.of(emailTemplate)));
    product.setEmailTemplates(mappings);

    // Execute
    Optional<EmailTemplate> result = product.getEmailTemplate("validType", "workflowType", status.name());

    // Verify
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(status, result.get().getStatus());
  }

  @Test
  void
  testGetEmailTemplate_WithDefault() {
    // Setup
    final var status = OnboardingStatus.PENDING;
    final var workflowType = WorkflowType.CONTRACT_REGISTRATION;
    Product product = new Product();
    Map<String, Map<String, List<EmailTemplate>>> mappings = new HashMap<>();
    EmailTemplate emailTemplate = new EmailTemplate();
    emailTemplate.setPath("path");
    emailTemplate.setStatus(status);
    mappings.put(INSTITUTION_TYPE_DEFAULT, Map.of(workflowType.name(), List.of(emailTemplate)));
    product.setEmailTemplates(mappings);

    // Execute
    var result = product.getEmailTemplate("unknownType", workflowType.name(), status.name());

    // Verify
    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void testGetEmailTemplate_NoMappings() {
    // Setup
    Product product = new Product();
    product.setEmailTemplates(null);

    // Execute
    var result = product.getEmailTemplate("anyType", "anyType", OnboardingStatus.DELETED.name());

    // Verify
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetEmailTemplate_WithNoBothKeys() {
    // Setup
    final var status = OnboardingStatus.PENDING;
    Product product = new Product();
    Map<String, Map<String, List<EmailTemplate>>> mappings = new HashMap<>();
    EmailTemplate emailTemplate = new EmailTemplate();
    emailTemplate.setPath("path");
    emailTemplate.setStatus(status);
    mappings.put(INSTITUTION_TYPE_DEFAULT, Map.of(WorkflowType.CONTRACT_REGISTRATION.name(), List.of(emailTemplate)));
    product.setEmailTemplates(mappings);

    // Execute
    var result = product.getEmailTemplate("unknownType", "test", status.name());

    // Verify
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
}
