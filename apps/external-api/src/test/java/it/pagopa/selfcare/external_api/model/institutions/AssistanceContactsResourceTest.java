package it.pagopa.selfcare.external_api.model.institutions;

import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.external_api.model.institution.AssistanceContactsResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistanceContactsResourceTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validateNotNullFields() {
        // given
        AssistanceContactsResource assistanceContactsResource = TestUtils.mockInstance(new AssistanceContactsResource());
        assistanceContactsResource.setSupportEmail("assistanceEmail@example.com");
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(assistanceContactsResource);
        // then
        assertTrue(violations.isEmpty());
    }

}
