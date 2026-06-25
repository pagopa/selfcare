package it.pagopa.selfcare.external_api;

import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;

import java.util.List;
import java.util.Map;

import static it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER;

public class TestUtils {

    public static Product dummyProduct(String productId) {
        Product product = new Product();
        product.setId(productId);
        ProductRole productRole = new ProductRole();
        productRole.setCode("admin");
        productRole.setLabel("Amministratore");
        productRole.setDescription("Amministratore");

        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        productRoleInfo.setRoles(List.of(productRole));

        product.setRoleMappings(Map.of(MANAGER, productRoleInfo));
        return product;
    }
}
