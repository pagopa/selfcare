package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistryManagerSELCTest {

    private Onboarding onboarding;
    private RegistryManagerSELC registryManagerSELC;

    @BeforeEach
    void setUp() {
        onboarding = mock(Onboarding.class);
        registryManagerSELC = new RegistryManagerSELC(onboarding);
    }

    @Test
    void givenInstitutionWithTaxCode_whenRetrieveInstitution_thenSetDefaultOriginAndOriginId() {
        // given
        var institution = mock(Institution.class);
        when(onboarding.getInstitution()).thenReturn(institution);
        when(institution.getTaxCode()).thenReturn("123456");

        // when
        registryManagerSELC.retrieveInstitution();

        // then
        verify(institution).setOriginId("123456");
        verify(institution).setOrigin(Origin.SELC);
    }

    @Test
    void givenAnyWorkflowType_whenCustomValidation_thenReturnOnboarding() {
        // when
        Uni<Onboarding> result = registryManagerSELC.customValidation(mock(ProductResponse.class));

        // then
        assertEquals(onboarding, result.await().indefinitely());
        verify(onboarding, never()).getWorkflowType();
    }

    @Test
    void givenAnyState_whenIsValid_thenReturnTrue() {
        // when
        Uni<Boolean> result = registryManagerSELC.isValid();

        // then
        assertTrue(result.await().indefinitely());
    }
}
