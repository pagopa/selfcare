package it.pagopa.selfcare.dashboard.model;

import it.pagopa.selfcare.commons.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportContactResourceTest {

    private Validator validator;
    private static final SupportContactResource SUPPORT_CONTACT_RESOURCE = TestUtils.mockInstance(new SupportContactResource());

    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }


    @Test
    void validateNotNullFields() {
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(SUPPORT_CONTACT_RESOURCE);
        // then
        assertTrue(violations.isEmpty());
    }

}