package it.pagopa.selfcare.external_api.model.institutions;

import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.external_api.model.institution.CompanyInformationsResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyInformationsResourceTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validateNotNullFields() {
        // given
        CompanyInformationsResource companyInformationsResource = TestUtils.mockInstance(new CompanyInformationsResource());
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(companyInformationsResource);
        // then
        assertTrue(violations.isEmpty());
    }

}
