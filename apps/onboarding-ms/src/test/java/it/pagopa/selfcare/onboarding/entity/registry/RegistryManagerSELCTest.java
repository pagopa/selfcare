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
import org.junit.jupiter.params.provider.NullSource;

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

    @ParameterizedTest
    @EnumSource(
            value = WorkflowType.class,
            names = {
                    "FOR_APPROVE",
                    "IMPORT",
                    "FOR_APPROVE_PT",
                    "FOR_APPROVE_GPU",
                    "CONFIRMATION",
                    "CONFIRMATION_AGGREGATOR",
                    "INCREMENT_REGISTRATION_AGGREGATOR",
                    "CONTRACT_WITH_COUNTERSIGNATURE"
            }
    )
    void givenAllowedWorkflowType_whenCustomValidation_thenReturnOnboarding(WorkflowType workflowType) {
        // given
        when(onboarding.getWorkflowType()).thenReturn(workflowType);

        // when
        Uni<Onboarding> result = registryManagerSELC.customValidation(mock(Product.class));

        // then
        assertEquals(onboarding, result.await().indefinitely());
    }

    @ParameterizedTest
    @EnumSource(
            value = WorkflowType.class,
            names = {
                    "FOR_APPROVE",
                    "IMPORT",
                    "FOR_APPROVE_PT",
                    "FOR_APPROVE_GPU",
                    "CONFIRMATION",
                    "CONFIRMATION_AGGREGATOR",
                    "INCREMENT_REGISTRATION_AGGREGATOR",
                    "CONTRACT_WITH_COUNTERSIGNATURE"
            }
    )
    void givenAllowedWorkflowType_whenIsWorkflowTypeAllowed_thenReturnTrue(WorkflowType workflowType) {
        // when
        boolean result = registryManagerSELC.isWorkflowTypeAllowed(workflowType);

        // then
        assertTrue(result);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(
            value = WorkflowType.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {
                    "FOR_APPROVE",
                    "IMPORT",
                    "FOR_APPROVE_PT",
                    "FOR_APPROVE_GPU",
                    "CONFIRMATION",
                    "CONFIRMATION_AGGREGATOR",
                    "INCREMENT_REGISTRATION_AGGREGATOR",
                    "CONTRACT_WITH_COUNTERSIGNATURE"
            }
    )
    void givenNotAllowedWorkflowType_whenIsWorkflowTypeAllowed_thenReturnFalse(WorkflowType workflowType) {
        // when
        boolean result = registryManagerSELC.isWorkflowTypeAllowed(workflowType);

        // then
        assertFalse(result);
    }

    @Test
    void givenNotAllowedWorkflowType_whenCustomValidation_thenThrowInvalidRequestException() {
        // given
        when(onboarding.getWorkflowType()).thenReturn(WorkflowType.CONTRACT_REGISTRATION);

        // when
        Uni<Onboarding> result = registryManagerSELC.customValidation(mock(Product.class));

        // then
        assertThrows(InvalidRequestException.class, () -> result.await().indefinitely());
    }

    @Test
    void givenAnyState_whenIsValid_thenReturnTrue() {
        // when
        Uni<Boolean> result = registryManagerSELC.isValid();

        // then
        assertTrue(result.await().indefinitely());
    }
}
