package it.pagopa.selfcare.external_api.utils;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeEnvironmentConfiguration;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractTemplateConfig;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.OnboardingType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RoleMapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductMsUtilsTest {

    @Test
    void mapsPartyRolesAndFallsBackToOperator() {
        assertEquals(PartyRole.MANAGER, ProductMsUtils.toPartyRole("MANAGER"));
        assertEquals(PartyRole.DELEGATE, ProductMsUtils.toPartyRole("DELEGATE"));
        assertEquals(PartyRole.SUB_DELEGATE, ProductMsUtils.toPartyRole("SUB_DELEGATE"));
        assertEquals(PartyRole.ADMIN_EA, ProductMsUtils.toPartyRole("ADMIN_EA"));
        assertEquals(PartyRole.OPERATOR, ProductMsUtils.toPartyRole("unexpected"));
    }

    @Test
    void selectsInstitutionRolesAndFallsBackToDefault() {
        RoleMapping fallback = role(InstitutionType.DEFAULT, "default");
        RoleMapping specific = role(InstitutionType.PA, "pa");
        ProductResponse product = product(List.of(fallback, specific));

        assertEquals("pa", ProductMsUtils.getRoleMappings(product, "PA").get(PartyRole.MANAGER).get(0).getBackOfficeRoles().get(0).getCode());
        assertEquals("default", ProductMsUtils.getRoleMappings(product, "GSP").get(PartyRole.MANAGER).get(0).getBackOfficeRoles().get(0).getCode());
        assertNull(ProductMsUtils.getRoleMappings(product(null), "PA"));
        assertTrue(ProductMsUtils.getRoleMappings(product(List.of()), "PA").isEmpty());
    }

    @Test
    void roleLookupPrefersDefaultAndThrowsForMissingRoleOrCode() {
        ProductResponse product = product(List.of(
                role(InstitutionType.PA, "same"),
                role(InstitutionType.DEFAULT, "same")));
        assertEquals("default", ProductMsUtils.getProductRole("same", PartyRole.MANAGER, product).getLabel());
        assertThrows(IllegalArgumentException.class, () -> ProductMsUtils.getProductRole("missing", PartyRole.MANAGER, product));
        assertThrows(IllegalArgumentException.class, () -> ProductMsUtils.getProductRole("same", PartyRole.OPERATOR, product));
    }

    @Test
    void findsInstitutionContractWithDefaultFallback() {
        ContractTemplateConfig defaultContract = contract(InstitutionType.DEFAULT, ContractType.CONTRACT, OnboardingType.INSTITUTION, "default");
        ContractTemplateConfig paContract = contract(InstitutionType.PA, ContractType.CONTRACT, OnboardingType.INSTITUTION, "pa");
        ContractTemplateConfig attachment = contract(InstitutionType.PA, ContractType.ATTACHMENT, OnboardingType.INSTITUTION, "attachment");
        ContractTemplateConfig userContract = contract(InstitutionType.PA, ContractType.CONTRACT, OnboardingType.USER, "user");
        ProductResponse product = product(null);
        product.setContracts(List.of(defaultContract, paContract, attachment, userContract));
        assertEquals("pa", ProductMsUtils.getInstitutionContract(product, "PA").orElseThrow().getPath());
        assertEquals("default", ProductMsUtils.getInstitutionContract(product, "GSP").orElseThrow().getPath());
        assertTrue(ProductMsUtils.getInstitutionContract(product(null), "PA").isEmpty());
    }

    @Test
    void findsProductionBackOfficeConfigurationCaseInsensitively() {
        BackOfficeEnvironmentConfiguration config = new BackOfficeEnvironmentConfiguration();
        config.setEnv("PROD");
        config.setUrlBO("https://bo.example");
        ProductResponse product = product(null);
        product.setBackOfficeEnvironmentConfigurations(List.of(config));
        assertEquals("https://bo.example", ProductMsUtils.getProdBackOfficeConfiguration(product).orElseThrow().getUrlBO());
        assertTrue(ProductMsUtils.getProdBackOfficeConfiguration(product(null)).isEmpty());
    }

    private static ProductResponse product(List<RoleMapping> mappings) {
        ProductResponse product = new ProductResponse();
        product.setRoleMappings(mappings);
        return product;
    }

    private static RoleMapping role(InstitutionType type, String code) {
        BackOfficeRole backOfficeRole = new BackOfficeRole();
        backOfficeRole.setCode(code);
        backOfficeRole.setLabel(type == InstitutionType.DEFAULT ? "default" : type == InstitutionType.PA ? "specific" : code);
        RoleMapping mapping = new RoleMapping();
        mapping.setRole("MANAGER");
        mapping.setInstitutionType(type);
        mapping.setBackOfficeRoles(List.of(backOfficeRole));
        return mapping;
    }

    private static ContractTemplateConfig contract(InstitutionType institutionType, ContractType contractType, OnboardingType onboardingType, String path) {
        ContractTemplateConfig contract = new ContractTemplateConfig();
        contract.setInstitutionType(institutionType);
        contract.setContractType(contractType);
        contract.setOnboardingType(onboardingType);
        contract.setPath(path);
        return contract;
    }
}



