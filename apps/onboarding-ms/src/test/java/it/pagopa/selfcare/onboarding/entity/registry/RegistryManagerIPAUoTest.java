package it.pagopa.selfcare.onboarding.entity.registry;

import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.party_registry_proxy_json.api.UoApi;

class RegistryManagerIPAUoTest {

    @Test
    void givenNoBillingAndNoRecipientCode_whenCustomValidation_thenThrowInvalidRequestException() {
        // given
        Onboarding onboarding = buildBaseOnboarding();
        onboarding.setProductId(PROD_IO_PREMIUM.getValue());
        onboarding.setBilling(null);
        RegistryManagerIPAUo registryManager = new RegistryManagerIPAUo(onboarding, mock(UoApi.class));

        // when
        Uni<Onboarding> result = registryManager.customValidation(mock(Product.class));

        // then
        assertThrows(InvalidRequestException.class, () -> result.await().indefinitely());
    }

    @Test
    void givenIoPremiumWithNotAllowedPricingPlan_whenCustomValidation_thenThrowInvalidRequestException() {
        // given
        Onboarding onboarding = buildBaseOnboarding();
        onboarding.setProductId(PROD_IO_PREMIUM.getValue());
        onboarding.setPricingPlan("C1");
        onboarding.getInstitution().setImported(true);
        RegistryManagerIPAUo registryManager = new RegistryManagerIPAUo(onboarding, mock(UoApi.class));

        // when
        Uni<Onboarding> result = registryManager.customValidation(mock(Product.class));

        // then
        assertThrows(InvalidRequestException.class, () -> result.await().indefinitely());
    }

    @Test
    void givenIoProductWithValidData_whenCustomValidation_thenReturnOnboarding() {
        // given
        Onboarding onboarding = buildBaseOnboarding();
        onboarding.setProductId(PROD_IO.getValue());
        onboarding.setPricingPlan("C0");
        RegistryManagerIPAUo registryManager = new RegistryManagerIPAUo(onboarding, mock(UoApi.class));

        // when
        Uni<Onboarding> result = registryManager.customValidation(mock(Product.class));

        // then
        assertEquals(onboarding, result.await().indefinitely());
    }

    @Test
    void givenCEDProductWithoutRecipientCode_whenCustomValidation_thenReturnOnboarding() {
        // given
        Onboarding onboarding = buildBaseOnboarding();
        onboarding.setProductId(PROD_CED.getValue());
        onboarding.setBilling(null);
        RegistryManagerIPAUo registryManager = new RegistryManagerIPAUo(onboarding, mock(UoApi.class));

        // when
        Uni<Onboarding> result = registryManager.customValidation(mock(Product.class));

        // then
        assertEquals(onboarding, result.await().indefinitely());
    }

    private Onboarding buildBaseOnboarding() {
        Institution institution = new Institution();
        institution.setInstitutionType(InstitutionType.PSP);
        institution.setTaxCode("12345678901");
        institution.setImported(false);

        Billing billing = new Billing();
        billing.setRecipientCode("ABC1234");

        Onboarding onboarding = new Onboarding();
        onboarding.setInstitution(institution);
        onboarding.setBilling(billing);
        onboarding.setPricingPlan("C0");
        return onboarding;
    }
}
