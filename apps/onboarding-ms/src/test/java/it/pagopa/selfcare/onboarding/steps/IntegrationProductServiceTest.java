package it.pagopa.selfcare.onboarding.steps;

import static org.junit.jupiter.api.Assertions.*;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.util.ProductConfigUtils;
import it.pagopa.selfcare.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.product_json.model.InstitutionType;
import org.openapi.quarkus.product_json.model.Origin;
import org.openapi.quarkus.product_json.model.WorkflowType;

class IntegrationProductServiceTest {
    private final IntegrationProductService service = new IntegrationProductService();

    @Test
    void loadsActualRolesContractsAndParentInsteadOfIdOnlyProducts() {
        var product = service.getValidProduct("prod-pagopa", "AR").await().indefinitely();
        var roles = ProductConfigUtils.roleMappings(product,
                it.pagopa.selfcare.onboarding.common.InstitutionType.PSP);
        assertEquals("admin-psp", roles.get(PartyRole.MANAGER).getBackOfficeRoles().get(0).getCode());
        assertEquals("1.0.0", ProductConfigUtils.institutionContractTemplate(product, "PSP").getVersion());
        assertEquals("prod-io", service.getValidProduct("prod-io-premium", "AR")
                .await().indefinitely().getParentId());
        assertEquals(30, service.getProductExpirationDays("prod-io", "AR").await().indefinitely());
    }

    @Test
    void sameIdIsDifferentAcrossTenantsWithoutFallback() {
        var ar = service.getValidProduct("prod-io", "AR").await().indefinitely();
        var pnpg = service.getValidProduct("prod-io", "PNPG").await().indefinitely();
        assertNotSame(ar, pnpg);
        assertEquals("AR", ar.getTenantId());
        assertEquals("PNPG", pnpg.getTenantId());
        assertNotEquals(ar.getTitle(), pnpg.getTitle());
        assertNotEquals(ar.getContracts().get(0).getPath(), pnpg.getContracts().get(0).getPath());
        assertEquals(45, service.getProductExpirationDays("prod-io", "PNPG").await().indefinitely());
        assertThrows(ResourceNotFoundException.class, () ->
                service.getProduct("prod-pagopa", "PNPG").await().indefinitely());
        assertThrows(ResourceNotFoundException.class, () ->
                service.getProduct("prod-pn-pg", "AR").await().indefinitely());
    }

    @Test
    void preservesWorkflowAndRequiredDocumentRulesPerTenant() {
        assertEquals(WorkflowType.CONTRACT_REGISTRATION, service.getWorkflowType(
                InstitutionType.PA, Origin.IPA, ProductId.PROD_IO, "AR").await().indefinitely().getWorkflowType());
        assertEquals(WorkflowType.FOR_APPROVE, service.getWorkflowType(
                InstitutionType.PG, Origin.INFOCAMERE, ProductId.PROD_IO, "PNPG").await().indefinitely().getWorkflowType());
        assertTrue(service.isRequiredDocuments(ProductId.PROD_PAGOPA, InstitutionType.GSP, Origin.SELC, "AR")
                .await().indefinitely());
        assertEquals("pnpg-doc", service.getRequiredDocuments(ProductId.PROD_IO,
                InstitutionType.PG, Origin.INFOCAMERE, "PNPG").await().indefinitely().get(0).getId());
        assertFalse(service.isRequiredDocuments(ProductId.PROD_IO, InstitutionType.PA, Origin.IPA, "AR")
                .await().indefinitely());
    }

    @Test
    void rejectsMissingInactiveAndPhasedOutProducts() {
        for (String id : new String[]{"missing", "test-test", "test-product2", "test-product-sub"}) {
            assertThrows(ResourceNotFoundException.class, () -> service.getValidProduct(id, "AR").await().indefinitely(), id);
        }
        assertNotNull(service.getValidProduct("test-product", "AR").await().indefinitely());
    }

    @Test
    void contextualTenantIsRequiredAndCannotConflictWithExplicitTenant() {
        service.tenantContext = new TenantContext();
        assertThrows(IllegalArgumentException.class, () -> service.getProduct("prod-io"));
        service.tenantContext.setTenantId("AR");
        assertEquals("AR", service.getProduct("prod-io").await().indefinitely().getTenantId());
        assertThrows(IllegalArgumentException.class, () -> service.getValidProduct("prod-io", "PNPG"));
        assertThrows(IllegalArgumentException.class, () -> service.getValidProduct("prod-io", "unknown"));
    }
}
