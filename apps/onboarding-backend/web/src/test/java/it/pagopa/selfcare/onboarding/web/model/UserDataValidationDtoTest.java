package it.pagopa.selfcare.onboarding.web.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.selfcare.commons.utils.TestUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserDataValidationDtoTest {

    private Validator validator;


    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validateNullFields() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("taxCode", NotBlank.class);
        UserDataValidationDto model = new UserDataValidationDto();
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(model);
        // then
        List<ConstraintViolation<Object>> filteredViolations = violations.stream()
                .filter(violation -> {
                    Class<? extends Annotation> annotationToCheck = toCheckMap.get(violation.getPropertyPath().toString());
                    return !violation.getConstraintDescriptor().getAnnotation().annotationType().equals(annotationToCheck);
                })
                .toList();
        assertTrue(filteredViolations.isEmpty());
    }

    @Test
    void validateNotNullFields() {
        // given
        UserDataValidationDto model = TestUtils.mockInstance(new UserDataValidationDto());
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(model);
        // then
        assertTrue(violations.isEmpty());
    }

}