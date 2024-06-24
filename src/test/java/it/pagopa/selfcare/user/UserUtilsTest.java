package it.pagopa.selfcare.user;

import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.TrackEventInput;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UserUtilsTest {

    @Test
    void groupingProductAndReturnMinStateProduct_whenActiveAndDelete() {

        List<OnboardedProduct> products = new ArrayList<>();
        products.add(dummyOnboardedProduct("example", OnboardedProductState.ACTIVE, 1));
        products.add(dummyOnboardedProduct("example", OnboardedProductState.DELETED, 1));

        Collection<OnboardedProduct> actuals = UserUtils.groupingProductAndReturnMinStateProduct(products);
        assertEquals(1, actuals.size());
        OnboardedProduct actual = actuals.stream().findFirst().orElse(null);
        assertNotNull(actual);
        assertNotNull(actual.getStatus());
        assertEquals(OnboardedProductState.ACTIVE, actual.getStatus());
    }

    @Test
    void groupingProductAndReturnMinStateProduct_whenMoreDelete() {

        List<OnboardedProduct> products = new ArrayList<>();
        products.add(dummyOnboardedProduct("example", OnboardedProductState.DELETED, 1));
        products.add(dummyOnboardedProduct("example", OnboardedProductState.DELETED, 2));
        OnboardedProduct expected = dummyOnboardedProduct("example", OnboardedProductState.DELETED, 3);
        products.add(expected);

        Collection<OnboardedProduct> actuals = UserUtils.groupingProductAndReturnMinStateProduct(products);

        assertEquals(1, actuals.size());
        OnboardedProduct actual = actuals.stream().findFirst().orElse(null);
        assertNotNull(actual);
        assertNotNull(actual.getStatus());
        assertEquals(expected, actual);
    }

    @Test
    void groupingProductAndReturnMinStateProduct_whenMoreProduct() {

        List<OnboardedProduct> products = new ArrayList<>();
        products.add(dummyOnboardedProduct("example", OnboardedProductState.DELETED, 1));
        products.add(dummyOnboardedProduct("example-2", OnboardedProductState.DELETED, 2));

        Collection<OnboardedProduct> actuals = UserUtils.groupingProductAndReturnMinStateProduct(products);
        assertEquals(2, actuals.size());
    }

    @Test
    void mapPropsForTrackEvent() {

        TrackEventInput trackEventInput = TrackEventInput.builder()
                .documentKey("documentKey")
                .userId("userId")
                .productId("productId")
                .institutionId("institutionId")
                .build();

        Map<String, String> maps = UserUtils.mapPropsForTrackEvent(trackEventInput);
        assertTrue(maps.containsKey("documentKey"));
        assertTrue(maps.containsKey("userId"));
        assertTrue(maps.containsKey("institutionId"));
        assertTrue(maps.containsKey("productId"));
    }

    OnboardedProduct dummyOnboardedProduct(String productRole, OnboardedProductState state, int day) {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId("productId");
        onboardedProduct.setProductRole(productRole);
        onboardedProduct.setCreatedAt(LocalDateTime.of(2024,1,day,0,0,0));
        onboardedProduct.setUpdatedAt(LocalDateTime.of(2024,1,day,0,0,0));
        onboardedProduct.setStatus(state);
        return onboardedProduct;
    }
}
