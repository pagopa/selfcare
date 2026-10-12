package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.product.ProductResource;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeEnvironmentConfiguration;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractTemplateConfig;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ContractType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.OnboardingType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductMetadata;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RoleMapping;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.VisualConfiguration;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductsMapperTest {

    private final ProductsMapper mapper = Mappers.getMapper(ProductsMapper.class);

    @Test
    void mapsProductMsResponseToExternalResource() {
        ProductResponse product = new ProductResponse();
        product.setProductId("prod-x");
        product.setTitle("Product X");
        product.setDescription("Description");
        product.setParentId("parent");
        VisualConfiguration visuals = new VisualConfiguration();
        visuals.setLogoUrl("logo");
        visuals.setDepictImageUrl("depict");
        visuals.setLogoBgColor("#fff");
        product.setVisualConfiguration(visuals);
        ProductMetadata metadata = new ProductMetadata();
        metadata.setCreatedAt(OffsetDateTime.parse("2025-12-01T09:00:00Z"));
        product.setMetadata(metadata);

        BackOfficeEnvironmentConfiguration backOffice = new BackOfficeEnvironmentConfiguration();
        backOffice.setEnv("PROD");
        backOffice.setUrlPublic("public");
        backOffice.setUrlBO("backoffice");
        backOffice.setIdentityTokenAudience("audience");
        product.setBackOfficeEnvironmentConfigurations(List.of(backOffice));

        ContractTemplateConfig contract = new ContractTemplateConfig();
        contract.setOnboardingType(OnboardingType.INSTITUTION);
        contract.setInstitutionType(InstitutionType.PA);
        contract.setContractType(ContractType.CONTRACT);
        contract.setPath("contracts/pa.html");
        contract.setVersion("2.0.0");
        product.setContracts(List.of(contract));

        BackOfficeRole backOfficeRole = new BackOfficeRole();
        backOfficeRole.setCode("admin");
        backOfficeRole.setLabel("Administrator");
        backOfficeRole.setProductLabel("Product administrator");
        backOfficeRole.setMultiroleGroups(List.of("group"));
        RoleMapping roleMapping = new RoleMapping();
        roleMapping.setRole("MANAGER");
        roleMapping.setInstitutionType(InstitutionType.DEFAULT);
        roleMapping.setBackOfficeRoles(List.of(backOfficeRole));
        roleMapping.setPhasesAdditionAllowed(List.of("onboarding"));
        roleMapping.setSkipUserCreation(true);
        product.setRoleMappings(List.of(roleMapping));

        ProductResource resource = mapper.toResource(product, "PA");
        assertEquals("prod-x", resource.getId());
        assertEquals("Product X", resource.getTitle());
        assertEquals("Description", resource.getDescription());
        assertEquals("parent", resource.getParentId());
        assertEquals("logo", resource.getLogo());
        assertEquals("depict", resource.getDepictImageUrl());
        assertEquals("#fff", resource.getLogoBgColor());
        assertEquals(Instant.parse("2025-12-01T09:00:00Z"), resource.getCreatedAt());
        assertEquals("public", resource.getUrlPublic());
        assertEquals("backoffice", resource.getUrlBO());
        assertEquals("audience", resource.getIdentityTokenAudience());
        assertEquals("contracts/pa.html", resource.getContractTemplatePath());
        assertEquals("2.0.0", resource.getContractTemplateVersion());
        assertNull(resource.getRoleManagementURL());
        assertTrue(resource.getRoleMappings().get(PartyRole.MANAGER).isMultiroleAllowed());
        assertTrue(resource.getRoleMappings().get(PartyRole.MANAGER).isSkipUserCreation());
        assertEquals("Product administrator", resource.getRoleMappings().get(PartyRole.MANAGER).getRoles().get(0).getProductLabel());
    }

    @Test
    void mapsMissingNestedValuesToNulls() {
        ProductResponse product = new ProductResponse();
        product.setProductId("bare");
        ProductResource resource = mapper.toResource(product, "PA");
        assertEquals("bare", resource.getId());
        assertNull(resource.getCreatedAt());
        assertNull(resource.getUrlBO());
        assertNull(resource.getContractTemplatePath());
        assertNull(resource.getRoleMappings());
    }
}

