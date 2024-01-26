package it.pagopa.selfcare.user.util;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.entity.ProductRole;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class UserUtilTest {

    @Inject
    private UserUtils userUtils;

    @InjectMock
    private ProductService productService;

    @Test
    void checkRoleValid(){

        when(productService.validateProductRole(any(), any(), any())).thenReturn(new ProductRole());
        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkProductRole("prod-pagopa", PartyRole.MANAGER, "operatore")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(Boolean.TRUE);
    }

    @Test
    void checkRoleProductRoleNotFound(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("RoleMappings map for product prod-pagopa not found"));
        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkProductRole("prod-pagopa", PartyRole.MANAGER, "amministratore")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class, "RoleMappings map for product prod-pagopa not found");
    }

    @Test
    void checkRoleProductRoleListNotExists(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("Role DELEGATE not found"));

        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkProductRole("prod-pagopa", PartyRole.DELEGATE, "operatore")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class, "Role DELEGATE not found");
    }

    @Test
    void checkProductRoleWithoutProductRole(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("ProductRole operatore not found for role MANAGER"));
        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkProductRole("prod-io", PartyRole.MANAGER, null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class, "ProductRole operatore not found for role MANAGER");

    }

    @Test
    void checkProductRoleWithoutRole(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("Role is mandatory to check productRole"));
        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkProductRole("prod-io", null, "operatore")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class, "Role is mandatory to check productRole");
    }

    @Test
    void checkRoleWithoutProduct(){
        when(productService.validateProductRole(any(), any(), any())).thenThrow(new IllegalArgumentException("ProductRole admin not found for role MANAGER"));
        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkProductRole("prod-io", PartyRole.MANAGER, "admin")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(Boolean.TRUE);
    }

    private Product getProductResource() {
        Product productResource = new Product();
        Map<PartyRole, ProductRoleInfo> map = new HashMap<>();
        ProductRoleInfo productRoleInfo = new ProductRoleInfo();
        ProductRole productRole = new ProductRole();
        productRole.setCode("operatore");
        productRoleInfo.setRoles(List.of(productRole));
        map.put(PartyRole.MANAGER, productRoleInfo);
        productResource.setRoleMappings(map);
        return productResource;
    }

}
