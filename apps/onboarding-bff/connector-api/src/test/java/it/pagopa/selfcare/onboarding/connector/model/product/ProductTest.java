package it.pagopa.selfcare.onboarding.connector.model.product;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    private static final String PA = "PA";
    private static final String GSP = "GSP";

    @Test
    void getInstitutionContractTemplate_returnsSpecificTemplateWhenInstitutionTypeIsMapped() {
        ContractTemplate paTemplate = template("pa.html");
        ContractTemplate defaultTemplate = template("default.html");
        Product product = new Product();
        product.setInstitutionContractMappings(Map.of(PA, paTemplate, Product.CONTRACT_TYPE_DEFAULT, defaultTemplate));

        assertSame(paTemplate, product.getInstitutionContractTemplate(PA));
    }

    @Test
    void getInstitutionContractTemplate_fallsBackToDefaultWhenInstitutionTypeIsNotMapped() {
        ContractTemplate defaultTemplate = template("default.html");
        Product product = new Product();
        product.setInstitutionContractMappings(Map.of(Product.CONTRACT_TYPE_DEFAULT, defaultTemplate));

        assertSame(defaultTemplate, product.getInstitutionContractTemplate(GSP));
    }

    @Test
    void getInstitutionContractTemplate_fallsBackToDefaultWhenInstitutionTypeIsNull() {
        ContractTemplate defaultTemplate = template("default.html");
        Product product = new Product();
        product.setInstitutionContractMappings(Map.of(Product.CONTRACT_TYPE_DEFAULT, defaultTemplate));

        assertSame(defaultTemplate, product.getInstitutionContractTemplate(null));
    }

    @Test
    void getInstitutionContractTemplate_returnsEmptyTemplateWhenNoMatchAndNoDefault() {
        Product product = new Product();
        product.setInstitutionContractMappings(Map.of(PA, template("pa.html")));

        ContractTemplate result = product.getInstitutionContractTemplate(GSP);

        assertNotNull(result);
        assertNull(result.getContractTemplatePath());
    }

    @Test
    void getContractTemplates_returnEmptyTemplateWhenMappingsAreNull() {
        Product product = new Product();

        assertNull(product.getInstitutionContractTemplate(PA).getContractTemplatePath());
        assertNull(product.getInstitutionAggregatorContractTemplate(PA).getContractTemplatePath());
        assertNull(product.getUserContractTemplate(PA).getContractTemplatePath());
        assertNull(product.getUserAggregatorContractTemplate(PA).getContractTemplatePath());
    }

    @Test
    void getContractTemplates_readFromTheirOwnMappings() {
        ContractTemplate institutionAggregator = template("institution-aggregator.html");
        ContractTemplate user = template("user.html");
        ContractTemplate userAggregator = template("user-aggregator.html");
        Product product = new Product();
        product.setInstitutionAggregatorContractMappings(Map.of(PA, institutionAggregator));
        product.setUserContractMappings(Map.of(PA, user));
        product.setUserAggregatorContractMappings(Map.of(PA, userAggregator));

        assertSame(institutionAggregator, product.getInstitutionAggregatorContractTemplate(PA));
        assertSame(user, product.getUserContractTemplate(PA));
        assertSame(userAggregator, product.getUserAggregatorContractTemplate(PA));
    }

    @Test
    void getRoleMappings_returnsInstitutionTypeSpecificMappings() {
        Map<PartyRole, ProductRoleInfo> global = Map.of(PartyRole.MANAGER, new ProductRoleInfo());
        Map<PartyRole, ProductRoleInfo> pa = Map.of(PartyRole.DELEGATE, new ProductRoleInfo());
        Product product = new Product();
        product.setRoleMappings(global);
        product.setRoleMappingsByInstitutionType(Map.of(PA, pa));

        assertSame(pa, product.getRoleMappings(PA));
    }

    @Test
    void getRoleMappings_fallsBackToGlobalMappingsWhenInstitutionTypeIsNotMapped() {
        Map<PartyRole, ProductRoleInfo> global = Map.of(PartyRole.MANAGER, new ProductRoleInfo());
        Product product = new Product();
        product.setRoleMappings(global);
        product.setRoleMappingsByInstitutionType(Map.of(PA, Map.of()));

        assertSame(global, product.getRoleMappings(GSP));
    }

    @Test
    void getRoleMappings_fallsBackToGlobalMappingsWhenInstitutionTypeIsNull() {
        Map<PartyRole, ProductRoleInfo> global = Map.of(PartyRole.MANAGER, new ProductRoleInfo());
        Product product = new Product();
        product.setRoleMappings(global);
        product.setRoleMappingsByInstitutionType(Map.of(PA, Map.of()));

        assertSame(global, product.getRoleMappings(null));
    }

    @Test
    void getRoleMappings_fallsBackToGlobalMappingsWhenSpecificMappingsAreNull() {
        Map<PartyRole, ProductRoleInfo> global = Map.of(PartyRole.MANAGER, new ProductRoleInfo());
        Product product = new Product();
        product.setRoleMappings(global);

        assertSame(global, product.getRoleMappings(PA));
    }

    private static ContractTemplate template(String path) {
        ContractTemplate template = new ContractTemplate();
        template.setContractTemplatePath(path);
        return template;
    }
}

