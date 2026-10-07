package it.pagopa.selfcare.onboarding.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static ContractTemplate template(String path) {
        ContractTemplate template = new ContractTemplate();
        template.setContractTemplatePath(path);
        return template;
    }

    private static ProductRoleInfo roleInfo(String code) {
        ProductRole role = new ProductRole();
        role.setCode(code);
        ProductRoleInfo info = new ProductRoleInfo();
        info.setRoles(List.of(role));
        return info;
    }

    @Test
    void contractTemplateIsSelectedByInstitutionType() {
        Product product = new Product();
        ContractTemplate pa = template("pa");
        product.setInstitutionContractMappings(Map.of("PA", pa, Product.CONTRACT_TYPE_DEFAULT, template("default")));

        assertSame(pa, product.getInstitutionContractTemplate("PA"));
    }

    @Test
    void contractTemplateFallsBackToDefaultForUnknownOrMissingInstitutionType() {
        Product product = new Product();
        ContractTemplate defaultTemplate = template("default");
        product.setUserContractMappings(Map.of("PA", template("pa"), Product.CONTRACT_TYPE_DEFAULT, defaultTemplate));

        assertSame(defaultTemplate, product.getUserContractTemplate("GSP"));
        assertSame(defaultTemplate, product.getUserContractTemplate(null));
    }

    @Test
    void contractTemplateIsEmptyWhenNoMappingMatchesAndThereIsNoDefault() {
        Product product = new Product();
        product.setUserAggregatorContractMappings(Map.of("PA", template("pa")));

        ContractTemplate fromMissingMap = product.getInstitutionAggregatorContractTemplate("PA");
        ContractTemplate fromNoDefault = product.getUserAggregatorContractTemplate("GSP");

        assertNotNull(fromMissingMap);
        assertNull(fromMissingMap.getContractTemplatePath());
        assertNotNull(fromNoDefault);
        assertNull(fromNoDefault.getContractTemplatePath());
    }

    @Test
    void eachContractMappingFamilyIsReadFromItsOwnMap() {
        Product product = new Product();
        product.setInstitutionContractMappings(Map.of("PA", template("institution")));
        product.setInstitutionAggregatorContractMappings(Map.of("PA", template("institution-aggregator")));
        product.setUserContractMappings(Map.of("PA", template("user")));
        product.setUserAggregatorContractMappings(Map.of("PA", template("user-aggregator")));

        assertEquals("institution", product.getInstitutionContractTemplate("PA").getContractTemplatePath());
        assertEquals(
                "institution-aggregator",
                product.getInstitutionAggregatorContractTemplate("PA").getContractTemplatePath());
        assertEquals("user", product.getUserContractTemplate("PA").getContractTemplatePath());
        assertEquals("user-aggregator", product.getUserAggregatorContractTemplate("PA").getContractTemplatePath());
    }

    @Test
    void roleMappingsPreferTheInstitutionTypeSpecificMap() {
        Product product = new Product();
        Map<PartyRole, ProductRoleInfo> byType = Map.of(PartyRole.MANAGER, roleInfo("admin-pa"));
        product.setRoleMappings(Map.of(PartyRole.MANAGER, roleInfo("admin")));
        product.setRoleMappingsByInstitutionType(Map.of("PA", byType));

        assertSame(byType, product.getRoleMappings("PA"));
    }

    @Test
    void roleMappingsFallBackToThePlainMap() {
        Product product = new Product();
        Map<PartyRole, ProductRoleInfo> plain = Map.of(PartyRole.MANAGER, roleInfo("admin"));
        product.setRoleMappings(plain);
        product.setRoleMappingsByInstitutionType(Map.of("PA", Map.of(PartyRole.MANAGER, roleInfo("admin-pa"))));

        assertSame(plain, product.getRoleMappings("GSP"));
        assertSame(plain, product.getRoleMappings(null));
    }

    @Test
    void roleMappingsFallBackToThePlainMapWhenThereAreNoInstitutionTypeMaps() {
        Product product = new Product();
        Map<PartyRole, ProductRoleInfo> plain = Map.of(PartyRole.MANAGER, roleInfo("admin"));
        product.setRoleMappings(plain);

        assertSame(plain, product.getRoleMappings("PA"));
    }

    @Test
    void roleMappingsAreNullWhenNothingIsConfigured() {
        assertNull(new Product().getRoleMappings("PA"));
    }
}
