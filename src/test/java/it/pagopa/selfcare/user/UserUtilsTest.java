package it.pagopa.selfcare.user;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.model.OnboardedProduct;
import it.pagopa.selfcare.user.model.TrackEventInput;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

public class UserUtilsTest {

    @Test
    void groupingProductAndReturnMinStateProduct_whenActiveAndDelete() {

        List<OnboardedProduct> products = new ArrayList<>();
        products.add(dummyOnboardedProduct("example", OnboardedProductState.ACTIVE));
        products.add(dummyOnboardedProduct("example", OnboardedProductState.DELETED));

        Collection<OnboardedProduct> actuals = UserUtils.groupingProductAndReturnMinStateProduct(products);
        assertEquals(1, actuals.size());
        OnboardedProduct actual = actuals.stream().findFirst().orElse(null);
        assertNotNull(actual);
        assertNotNull(actual.getStatus());
        assertEquals(OnboardedProductState.ACTIVE, actual.getStatus());
    }

    @Test
    void groupingProductAndRoleAndReturnMinStateProduct_whenActiveAndDelete() {

        List<OnboardedProduct> products = new ArrayList<>();
        products.add(dummyOnboardedProductWithRole("example", OnboardedProductState.ACTIVE, PartyRole.MANAGER));
        products.add(dummyOnboardedProductWithRole("example", OnboardedProductState.DELETED, PartyRole.MANAGER));

        Collection<OnboardedProduct> actuals = UserUtils.groupingProductWithRoleAndReturnMinStateProduct(products);
        assertEquals(1, actuals.size());
        OnboardedProduct actual = actuals.stream().findFirst().orElse(null);
        assertNotNull(actual);
        assertNotNull(actual.getStatus());
        assertEquals(OnboardedProductState.ACTIVE, actual.getStatus());
    }

    @Test
    void groupingProductAndReturnMinStateProduct_whenMoreRole() {

        List<OnboardedProduct> products = new ArrayList<>();
        products.add(dummyOnboardedProductWithRole("example", OnboardedProductState.ACTIVE, PartyRole.MANAGER));
        products.add(dummyOnboardedProductWithRole("example", OnboardedProductState.DELETED, PartyRole.DELEGATE));
        products.add(dummyOnboardedProductWithRole("example", OnboardedProductState.DELETED, PartyRole.SUB_DELEGATE));

        Collection<OnboardedProduct> actuals = UserUtils.groupingProductWithRoleAndReturnMinStateProduct(products);

        assertNotNull(actuals);
        assertEquals(3, actuals.size());

        OnboardedProduct[] ps = products.toArray(new OnboardedProduct[0]);
        BiFunction<OnboardedProduct, OnboardedProduct, Boolean> getPredicate = (OnboardedProduct a, OnboardedProduct p) -> a.getStatus().equals(p.getStatus()) && a.getRole().equals(p.getRole());
        assertTrue(actuals.stream().anyMatch(act -> getPredicate.apply(act, ps[0])));
        assertTrue(actuals.stream().anyMatch(act -> getPredicate.apply(act, ps[1])));
        assertTrue(actuals.stream().anyMatch(act -> getPredicate.apply(act, ps[2])));
    }

    @Test
    void groupingProductAndReturnMinStateProduct_whenMoreDelete() {

        List<OnboardedProduct> products = new ArrayList<>();
        products.add(dummyOnboardedProduct("example", OnboardedProductState.DELETED));
        products.add(dummyOnboardedProduct("example", OnboardedProductState.DELETED));
        OnboardedProduct expected = dummyOnboardedProduct("example", OnboardedProductState.DELETED);
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
        products.add(dummyOnboardedProduct("example", OnboardedProductState.DELETED));
        products.add(dummyOnboardedProduct("example-2", OnboardedProductState.DELETED));

        Collection<OnboardedProduct> actuals = UserUtils.groupingProductAndReturnMinStateProduct(products);
        assertEquals(2, actuals.size());
    }

    @Test
    void mapPropsForTrackEvent() {

        TrackEventInput trackEventInput = TrackEventInput.builder().documentKey("documentKey").userId("userId").productId("productId").institutionId("institutionId").build();

        Map<String, String> maps = UserUtils.mapPropsForTrackEvent(trackEventInput);
        assertTrue(maps.containsKey("documentKey"));
        assertTrue(maps.containsKey("userId"));
        assertTrue(maps.containsKey("institutionId"));
        assertTrue(maps.containsKey("productId"));
    }

    OnboardedProduct dummyOnboardedProductWithRole(String productRole, OnboardedProductState state, PartyRole role) {
        OnboardedProduct onboardedProduct = dummyOnboardedProduct(productRole, state);
        onboardedProduct.setRole(role);
        return onboardedProduct;
    }

    OnboardedProduct dummyOnboardedProduct(String productRole, OnboardedProductState state) {
        OnboardedProduct onboardedProduct = new OnboardedProduct();
        onboardedProduct.setProductId("productId");
        onboardedProduct.setProductRole(productRole);
        onboardedProduct.setCreatedAt(OffsetDateTime.of(LocalDate.EPOCH, LocalTime.MIN, ZoneOffset.UTC));
        onboardedProduct.setUpdatedAt(OffsetDateTime.of(LocalDate.EPOCH, LocalTime.MIN, ZoneOffset.UTC));
        onboardedProduct.setStatus(state);
        return onboardedProduct;
    }
}
