package it.pagopa.selfcare.onboarding.connector.rest.mapper;

import it.pagopa.selfcare.onboarding.connector.model.product.Product;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractTemplateConfig;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.OnboardingType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RoleMapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

