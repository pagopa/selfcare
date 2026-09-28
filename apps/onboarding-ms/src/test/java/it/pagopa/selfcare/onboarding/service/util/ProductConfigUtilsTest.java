package it.pagopa.selfcare.onboarding.service.util;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.model.ContractTemplateConfig;
import org.openapi.quarkus.product_json.model.ContractType;
import org.openapi.quarkus.product_json.model.Features;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.product_json.model.RoleMapping;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductConfigUtilsTest {

    @Test
    void roleMappings_useSpecificInstitutionWithoutMergingGlobalOrOtherInstitutions() {
        RoleMapping specific = role("PA", "MANAGER", "onboarding");
        ProductResponse product = product(
                role("GSP", "DELEGATE", "onboarding"),
                role("DEFAULT", "OPERATOR", "onboarding"),
                specific);

        assertEquals(Map.of(PartyRole.MANAGER, specific), ProductConfigUtils.roleMappings(product, InstitutionType.PA));
    }

    @Test
    void roleMappings_useGlobalAndDefaultOnlyWhenSpecificInstitutionIsMissing() {
        RoleMapping global = role(null, "MANAGER", "onboarding");
        RoleMapping defaultRole = role("DEFAULT", "DELEGATE", "onboarding");
        ProductResponse product = product(global, defaultRole, role("GSP", "OPERATOR", "onboarding"));

        assertEquals(Map.of(PartyRole.MANAGER, global, PartyRole.DELEGATE, defaultRole),
                ProductConfigUtils.roleMappings(product, InstitutionType.PA));
    }

    @Test
    void roleMappings_rejectOtherInstitutionFallback() {
        ProductResponse product = product(role("GSP", "MANAGER", "onboarding"));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ProductConfigUtils.roleMappings(product, InstitutionType.PA));
        assertTrue(error.getMessage().contains("prod-io"));
        assertTrue(error.getMessage().contains("PA"));
    }

    @Test
    void roleMappings_distinguishMissingConfigurationFromExplicitEmptyConfiguration() {
        ProductResponse missing = new ProductResponse().productId("prod-io");
        missing.setRoleMappings(null);
        assertThrows(IllegalStateException.class, () -> ProductConfigUtils.roleMappings(missing, InstitutionType.PA));
        assertThrows(IllegalStateException.class, () -> ProductConfigUtils.roleMappings(null, InstitutionType.PA));
        assertEquals(Map.of(), ProductConfigUtils.roleMappings(product(), InstitutionType.PA));
        assertEquals(List.of(), ProductConfigUtils.validRoles(product(), InstitutionType.PA));
    }

    @Test
    void roleMappings_rejectMalformedEntries() {
        for (RoleMapping malformed : Arrays.asList(null, role("PA", null), role("PA", "unknown"))) {
            assertThrows(IllegalStateException.class,
                    () -> ProductConfigUtils.roleMappings(product(malformed), InstitutionType.PA));
        }
    }

    @Test
    void validRoles_preserveOnboardingPhaseFiltering() {
        ProductResponse product = product(
                role("PA", "MANAGER", "OnBoArDiNg"),
                role("PA", "DELEGATE", "management"),
                role("PA", "OPERATOR"));

        assertEquals(List.of(PartyRole.MANAGER), ProductConfigUtils.validRoles(product, InstitutionType.PA));
    }

    @Test
    void ptOnboarding_keepsRegularRoleMappingsSeparateFromPartnerTechMappings() {
        RoleMapping onboardingRole = role("PT", "MANAGER", "onboarding");
        ProductResponse product = product(onboardingRole);
        product.setPartnerTechRoleMappings(List.of(role("PT", "DELEGATE", "onboarding")));

        assertEquals(Map.of(PartyRole.MANAGER, onboardingRole),
                ProductConfigUtils.roleMappings(product, InstitutionType.PT));
    }

    @Test
    void contractTemplate_selectsSpecificContractThenDefaultButNotAnotherType() {
        ContractTemplateConfig global = contract(null, "global", "1");
        ContractTemplateConfig specific = contract("PA", "specific", "2");
        ProductResponse product = product();
        product.setContracts(List.of(global, specific));

        assertSame(specific, ProductConfigUtils.institutionContractTemplate(product, "PA"));
        assertSame(global, ProductConfigUtils.institutionContractTemplate(product, "PT"));

        product.setContracts(List.of(specific));
        ContractTemplateConfig missing = ProductConfigUtils.institutionContractTemplate(product, "PT");
        assertNull(missing.getPath());
        assertNull(missing.getVersion());
    }

    @Test
    void contractTemplate_keepsOptionalMetadataWithoutSwitchingToDefault() {
        ProductResponse product = product();
        product.setContracts(List.of(contract("DEFAULT", "default", "1"), contract("PA", "specific", null)));

        assertEquals("specific", ProductConfigUtils.institutionContractTemplate(product, "PA").getPath());
        assertNull(ProductConfigUtils.institutionContractTemplate(product, "PA").getVersion());
        assertEquals("default", ProductConfigUtils.institutionContractTemplate(product, "PT").getPath());
    }

    @Test
    void productFeatures_keepDefaultsAndConfiguredValues() {
        assertEquals(30, ProductConfigUtils.expirationDays(null));
        assertEquals(30, ProductConfigUtils.expirationDays(product()));
        assertFalse(ProductConfigUtils.delegable(null));
        assertFalse(ProductConfigUtils.delegable(product()));

        ProductResponse configured = product().features(new Features().expirationDays(0).delegable(true));
        assertEquals(0, ProductConfigUtils.expirationDays(configured));
        assertTrue(ProductConfigUtils.delegable(configured));
    }

    private ProductResponse product(RoleMapping... roles) {
        return new ProductResponse().productId("prod-io").roleMappings(Arrays.asList(roles));
    }

    private RoleMapping role(String institutionType, String role, String... phases) {
        RoleMapping mapping = new RoleMapping().role(role).phasesAdditionAllowed(List.of(phases));
        if (institutionType != null) {
            mapping.setInstitutionType(org.openapi.quarkus.product_json.model.InstitutionType.valueOf(institutionType));
        }
        return mapping;
    }

    private ContractTemplateConfig contract(String institutionType, String path, String version) {
        ContractTemplateConfig template = new ContractTemplateConfig()
                .contractType(ContractType.CONTRACT).path(path).version(version);
        if (institutionType != null) {
            template.setInstitutionType(org.openapi.quarkus.product_json.model.InstitutionType.valueOf(institutionType));
        }
        return template;
    }
}
