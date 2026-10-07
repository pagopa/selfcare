package it.pagopa.selfcare.external_api;

import it.pagopa.selfcare.product.generated.openapi.v1.dto.BackOfficeRole;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.InstitutionType;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.ProductResponse;
import it.pagopa.selfcare.product.generated.openapi.v1.dto.RoleMapping;

import java.util.List;
import java.util.ArrayList;

public class TestUtils {

    public static ProductResponse dummyProductResponse(String productId) {
        BackOfficeRole role = new BackOfficeRole();
        role.setCode("admin");
        role.setLabel("Amministratore");
        role.setDescription("Amministratore");

        RoleMapping mapping = new RoleMapping();
        mapping.setRole("MANAGER");
        mapping.setInstitutionType(InstitutionType.DEFAULT);
        mapping.setBackOfficeRoles(new ArrayList<>(List.of(role)));

        ProductResponse product = new ProductResponse();
        product.setProductId(productId);
        product.setRoleMappings(new ArrayList<>(List.of(mapping)));
        return product;
    }
}
