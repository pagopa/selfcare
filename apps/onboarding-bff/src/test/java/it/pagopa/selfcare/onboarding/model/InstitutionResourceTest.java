package it.pagopa.selfcare.onboarding.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class InstitutionResourceTest {

    @Test
    void classExistsInNewModel() {
        assertDoesNotThrow(() -> Class.forName("it.pagopa.selfcare.onboarding.controller.response.InstitutionResource"));
    }
}
