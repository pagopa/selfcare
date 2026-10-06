package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.connector.model.product.AttachmentTemplate;
import it.pagopa.selfcare.onboarding.connector.model.product.ContractTemplate;
import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import it.pagopa.selfcare.onboarding.connector.model.product.ProductRoleInfo;
import it.pagopa.selfcare.onboarding.connector.model.product.StorageOrigin;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractTemplateConfig;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.Features;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.OnboardingType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductStatus;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RoleMapping;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.VisualConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductMapperTest {

    private final ProductMapper mapper = new RestProductMapperImpl();

    @Test
    void mapsEnabledUserContractAndSkipsDisabledContract() {
        ContractTemplateConfig enabledContract = contract(true, "user-contract.html");
        enabledContract.setOnboardingType(OnboardingType.USER);
        ContractTemplateConfig disabledContract = contract(false, "disabled-contract.html");
        ProductResponse response = new ProductResponse();
        response.setContracts(List.of(enabledContract, disabledContract));

        Product product = mapper.toProduct(response);

        assertEquals("user-contract.html", product.getUserContractMappings().get("DEFAULT").getContractTemplatePath());
        assertTrue(product.getInstitutionContractMappings().isEmpty());
        assertTrue(product.getInstitutionAggregatorContractMappings().isEmpty());
        assertTrue(product.getUserAggregatorContractMappings().isEmpty());
    }

    @Test
    void initializesContractMappingsWhenResponseHasNoContracts() {
        Product product = mapper.toProduct(new ProductResponse());

        assertNotNull(product.getInstitutionContractMappings());
        assertNotNull(product.getInstitutionAggregatorContractMappings());
        assertNotNull(product.getUserContractMappings());
        assertNotNull(product.getUserAggregatorContractMappings());
    }

    @Test
    void mapsRoleMappingsByInstitutionType() {
        BackOfficeRole backOfficeRole = new BackOfficeRole();
        backOfficeRole.setCode("admin");
        RoleMapping roleMapping = new RoleMapping();
        roleMapping.setRole("ADMIN_EA");
        roleMapping.setInstitutionType(InstitutionType.PRV);
        roleMapping.setBackOfficeRoles(List.of(backOfficeRole));
        ProductResponse response = new ProductResponse();
        response.setRoleMappings(List.of(roleMapping));

        Product product = mapper.toProduct(response);

        assertEquals("admin", product.getRoleMappings("PRV").get(PartyRole.ADMIN_EA).getRoles().get(0).getCode());
    }

    @Test
    void toProduct_nullResponseThrows() {
        assertThrows(NullPointerException.class, () -> mapper.toProduct(null));
    }

    @Test
    void toProduct_mapsBaseFieldsVisualConfigurationAndFeatures() {
        VisualConfiguration visual = new VisualConfiguration();
        visual.setLogoUrl("logo.png");
        visual.setDepictImageUrl("depict.png");
        visual.setLogoBgColor("#FFFFFF");

        Features features = new Features();
        features.setEnabled(true);
        features.setDelegable(true);
        features.setInvoiceable(true);
        features.setRequiresParentOnboarding(true);
        features.setAllowCompanyOnboarding(true);
        features.setAllowIndividualOnboarding(true);
        features.setAllowedInstitutionTaxCode(List.of("ABC123"));
        features.setExpirationDays(30);

        ProductResponse response = new ProductResponse();
        response.setProductId("prod-io");
        response.setTitle("App IO");
        response.setDescription("description");
        response.setParentId("parent");
        response.setAlias("alias");
        response.setStatus(ProductStatus.ACTIVE);
        response.setInstitutionTypesAllowed(List.of("PA"));
        response.setConsumers(List.of("STANDARD"));
        response.setTestEnvProductIds(List.of("prod-io-coll"));
        response.setVisualConfiguration(visual);
        response.setFeatures(features);

        Product product = mapper.toProduct(response);

        assertEquals("prod-io", product.getId());
        assertEquals("App IO", product.getTitle());
        assertEquals("description", product.getDescription());
        assertEquals("parent", product.getParentId());
        assertEquals("alias", product.getAlias());
        assertEquals(it.pagopa.selfcare.onboarding.connector.model.product.ProductStatus.ACTIVE, product.getStatus());
        assertEquals(List.of("PA"), product.getInstitutionTypesAllowed());
        assertEquals(List.of("STANDARD"), product.getConsumers());
        assertEquals(List.of("prod-io-coll"), product.getTestEnvProductIds());
        assertEquals("logo.png", product.getLogo());
        assertEquals("depict.png", product.getDepictImageUrl());
        assertEquals("#FFFFFF", product.getLogoBgColor());
        assertTrue(product.isEnabled());
        assertTrue(product.isDelegable());
        assertTrue(product.isInvoiceable());
        assertTrue(product.isRequiresParentOnboarding());
        assertTrue(product.isAllowCompanyOnboarding());
        assertTrue(product.isAllowIndividualOnboarding());
        assertEquals(List.of("ABC123"), product.getAllowedInstitutionTaxCode());
        assertEquals(30, product.getExpirationDate());
    }

    @Test
    void toProduct_withoutOptionalSectionsLeavesDefaults() {
        Product product = mapper.toProduct(new ProductResponse());

        assertNull(product.getStatus());
        assertNull(product.getLogo());
        assertFalse(product.isEnabled());
        assertNull(product.getAllowedInstitutionTaxCode());
        assertTrue(product.getRoleMappings().isEmpty());
        assertTrue(product.getRoleMappingsByInstitutionType().isEmpty());
    }

    @Test
    void toProduct_nullRoleMappingsLeavesRoleMappingsUnset() {
        ProductResponse response = new ProductResponse();
        response.setRoleMappings(null);

        Product product = mapper.toProduct(response);

        assertNull(product.getRoleMappings());
        assertNull(product.getRoleMappingsByInstitutionType());
    }

    @Test
    void toProduct_mapsFeaturesWithNullFlagsAsFalse() {
        ProductResponse response = new ProductResponse();
        response.setFeatures(new Features());

        Product product = mapper.toProduct(response);

        assertFalse(product.isEnabled());
        assertFalse(product.isDelegable());
        assertFalse(product.isInvoiceable());
        assertFalse(product.isRequiresParentOnboarding());
        assertFalse(product.isAllowCompanyOnboarding());
        assertFalse(product.isAllowIndividualOnboarding());
        assertNull(product.getExpirationDate());
    }

    @Test
    void toProduct_routesContractsByOnboardingTypeAndDefaults() {
        ContractTemplateConfig aggregator = contract(true, "institution-aggregator.html");
        aggregator.setOnboardingType(OnboardingType.INSTITUTION_AGGREGATOR);
        aggregator.setInstitutionType(InstitutionType.PA);
        ContractTemplateConfig userAggregator = contract(true, "user-aggregator.html");
        userAggregator.setOnboardingType(OnboardingType.USER_AGGREGATOR);
        ContractTemplateConfig withoutTypes = contract(true, "no-types.html");
        withoutTypes.setOnboardingType(null);
        withoutTypes.setInstitutionType(null);
        withoutTypes.setContractType(null);
        withoutTypes.setVersion("1.0.0");
        withoutTypes.setEnabled(null);

        ProductResponse response = new ProductResponse();
        List<ContractTemplateConfig> contracts = new ArrayList<>();
        contracts.add(null);
        contracts.add(aggregator);
        contracts.add(userAggregator);
        contracts.add(withoutTypes);
        response.setContracts(contracts);

        Product product = mapper.toProduct(response);

        assertEquals("institution-aggregator.html",
                product.getInstitutionAggregatorContractMappings().get("PA").getContractTemplatePath());
        assertEquals("user-aggregator.html",
                product.getUserAggregatorContractMappings().get("DEFAULT").getContractTemplatePath());
        ContractTemplate institution = product.getInstitutionContractMappings().get("DEFAULT");
        assertEquals("no-types.html", institution.getContractTemplatePath());
        assertEquals("1.0.0", institution.getContractTemplateVersion());
        assertTrue(product.getUserContractMappings().isEmpty());
    }

    @Test
    void toProduct_mapsAttachmentsOnSameTemplate() {
        ContractTemplateConfig contract = contract(true, "contract.html");
        ContractTemplateConfig fullAttachment = attachment("Allegato A", "attachment-a.pdf");
        fullAttachment.setVersion("2.0.0");
        fullAttachment.setMandatory(true);
        fullAttachment.setGenerated(true);
        fullAttachment.setOrder(3);
        fullAttachment.setWorkflowType(List.of(
                it.pagopa.selfcare.product.generated.openapi.v1.dto.WorkflowType.CONTRACT_REGISTRATION));
        fullAttachment.setWorkflowState("PENDING");
        ContractTemplateConfig minimalAttachment = attachment("Allegato B", "attachment-b.pdf");
        ContractTemplateConfig unnamedAttachment = attachment(null, "unnamed.pdf");

        ProductResponse response = new ProductResponse();
        response.setContracts(List.of(contract, fullAttachment, minimalAttachment, unnamedAttachment));

        Product product = mapper.toProduct(response);

        ContractTemplate template = product.getInstitutionContractMappings().get("DEFAULT");
        assertEquals("contract.html", template.getContractTemplatePath());
        assertEquals(2, template.getAttachments().size());

        AttachmentTemplate first = template.getAttachments().get(0);
        assertEquals("Allegato A", first.getName());
        assertEquals("attachment-a.pdf", first.getTemplatePath());
        assertEquals("2.0.0", first.getTemplateVersion());
        assertTrue(first.isMandatory());
        assertTrue(first.isGenerated());
        assertEquals(3, first.getOrder());
        assertEquals(List.of(WorkflowType.CONTRACT_REGISTRATION), first.getWorkflowType());
        assertEquals(OnboardingStatus.PENDING, first.getWorkflowState());

        AttachmentTemplate second = template.getAttachments().get(1);
        assertEquals("Allegato B", second.getName());
        assertFalse(second.isMandatory());
        assertFalse(second.isGenerated());
        assertEquals(0, second.getOrder());
        assertTrue(second.getWorkflowType().isEmpty());
        assertNull(second.getWorkflowState());
    }

    @Test
    void toProduct_mapsGlobalRoleMappingsAndMergesRolesOfSameRole() {
        BackOfficeRole admin = new BackOfficeRole();
        admin.setCode("admin");
        admin.setLabel("Amministratore");
        admin.setDescription("desc");
        admin.setProductLabel("Product admin");
        admin.setMultiroleGroups(List.of("group"));
        BackOfficeRole operator = new BackOfficeRole();
        operator.setCode("operator");

        RoleMapping first = new RoleMapping();
        first.setRole("MANAGER");
        first.setBackOfficeRoles(List.of(admin));
        first.setPhasesAdditionAllowed(List.of("onboarding"));
        first.setSkipUserCreation(true);
        first.setExcludeRoleFromUserGroups(true);
        RoleMapping second = new RoleMapping();
        second.setRole("MANAGER");
        second.setInstitutionType(InstitutionType.DEFAULT);
        second.setBackOfficeRoles(List.of(operator));
        RoleMapping withoutBackOfficeRoles = new RoleMapping();
        withoutBackOfficeRoles.setRole("DELEGATE");
        RoleMapping withoutRole = new RoleMapping();

        ProductResponse response = new ProductResponse();
        response.setRoleMappings(List.of(first, second, withoutBackOfficeRoles, withoutRole));

        Product product = mapper.toProduct(response);

        ProductRoleInfo manager = product.getRoleMappings().get(PartyRole.MANAGER);
        assertEquals(2, manager.getRoles().size());
        assertEquals("admin", manager.getRoles().get(0).getCode());
        assertEquals("Amministratore", manager.getRoles().get(0).getLabel());
        assertEquals("desc", manager.getRoles().get(0).getDescription());
        assertEquals("Product admin", manager.getRoles().get(0).getProductLabel());
        assertEquals(List.of("group"), manager.getRoles().get(0).getMultiroleGroups());
        assertEquals("operator", manager.getRoles().get(1).getCode());
        // the last mapping for the same role wins for the flags
        assertFalse(manager.isSkipUserCreation());
        assertFalse(manager.isExcludeRoleFromUserGroups());

        ProductRoleInfo delegate = product.getRoleMappings().get(PartyRole.DELEGATE);
        assertTrue(delegate.getRoles().isEmpty());
        assertEquals(2, product.getRoleMappings().size());
        assertTrue(product.getRoleMappingsByInstitutionType().isEmpty());
    }

    @Test
    void toProduct_mapsRoleMappingFlags() {
        RoleMapping mapping = new RoleMapping();
        mapping.setRole("MANAGER");
        mapping.setSkipUserCreation(true);
        mapping.setExcludeRoleFromUserGroups(true);
        mapping.setPhasesAdditionAllowed(List.of("dashboard"));
        ProductResponse response = new ProductResponse();
        response.setRoleMappings(List.of(mapping));

        ProductRoleInfo roleInfo = mapper.toProduct(response).getRoleMappings().get(PartyRole.MANAGER);

        assertTrue(roleInfo.isSkipUserCreation());
        assertTrue(roleInfo.isExcludeRoleFromUserGroups());
        assertEquals(List.of("dashboard"), roleInfo.getPhasesAdditionAllowed());
    }

    @Test
    void toStorageOrigin_mapsValueAndNull() {
        assertEquals(StorageOrigin.SYSTEM, mapper.toStorageOrigin("SYSTEM"));
        assertEquals(StorageOrigin.USER, mapper.toStorageOrigin("USER"));
        assertNull(mapper.toStorageOrigin(null));
    }

    private ContractTemplateConfig attachment(String name, String path) {
        ContractTemplateConfig config = contract(true, path);
        config.setContractType(ContractType.ATTACHMENT);
        config.setName(name);
        return config;
    }

    private ContractTemplateConfig contract(boolean enabled, String path) {
        ContractTemplateConfig config = new ContractTemplateConfig();
        config.setEnabled(enabled);
        config.setOnboardingType(OnboardingType.INSTITUTION);
        config.setInstitutionType(InstitutionType.DEFAULT);
        config.setContractType(ContractType.CONTRACT);
        config.setPath(path);
        return config;
    }
}

