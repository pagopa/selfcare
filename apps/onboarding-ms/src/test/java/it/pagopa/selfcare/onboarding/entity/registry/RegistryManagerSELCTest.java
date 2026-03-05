package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
    void retrieveInstitution_shouldSetDefaultOriginAndOriginId() {
        var institution = mock(Institution.class);
        when(onboarding.getInstitution()).thenReturn(institution);
        when(institution.getTaxCode()).thenReturn("123456");

        registryManagerSELC.retrieveInstitution();

        verify(institution).setOriginId("123456");
        verify(institution).setOrigin(Origin.SELC);
    }

    @ParameterizedTest
    @EnumSource(value = WorkflowType.class, names = {"FOR_APPROVE", "IMPORT", "FOR_APPROVE_PT"})
    void customValidation_shouldReturnOnboarding_whenWorkflowTypeAllowed(WorkflowType workflowType) {
        when(onboarding.getWorkflowType()).thenReturn(workflowType);

        Uni<Onboarding> result = registryManagerSELC.customValidation(mock(Product.class));

        assertEquals(onboarding, result.await().indefinitely());
    }

    @Test
    void customValidation_shouldThrowInvalidRequestException_whenWorkflowTypeNotAllowed() {
        when(onboarding.getWorkflowType()).thenReturn(WorkflowType.CONTRACT_REGISTRATION);

        Uni<Onboarding> result = registryManagerSELC.customValidation(mock(Product.class));

        assertThrows(InvalidRequestException.class, () -> result.await().indefinitely());
    }

    @Test
    void isValid_shouldReturnTrue() {
        Uni<Boolean> result = registryManagerSELC.isValid();

        assertTrue(result.await().indefinitely());
    }
}