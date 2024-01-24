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

        when(productService.getProduct(any())).thenReturn(getProductResource());

        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkRoles("prod-pagopa", PartyRole.MANAGER, "operatore")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(Boolean.TRUE);
    }

    @Test
    void checkRoleProductRoleNotFound(){
        when(productService.getProduct(any())).thenReturn(getProductResource());
        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkRoles("prod-pagopa", PartyRole.MANAGER, "amministratore")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class, "PRODUCT_ROLE_NOT_FOUND");

    }

    @Test
    void checkRoleProductRoleListNotExists(){
        when(productService.getProduct(any())).thenReturn(getProductResource());

        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkRoles("prod-pagopa", PartyRole.DELEGATE, "operatore")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class, "ROLE_NOT_FOUND");
    }

    @Test
    void checkRoleWithoutProductRole(){
        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkRoles("prod-io", PartyRole.MANAGER, null)
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertCompleted().assertItem(Boolean.TRUE);
    }

    @Test
    void checkRoleWithoutRole(){
        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkRoles("prod-io", null, "operatore")
                .subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        subscriber.assertFailedWith(InvalidRequestException.class, "ROLE_IS_NULL - Role is required if productRole is present");
    }

    @Test
    void checkRoleWithoutProduct(){
        UniAssertSubscriber<Boolean> subscriber = userUtils
                .checkRoles("prod-io", PartyRole.MANAGER, null)
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
