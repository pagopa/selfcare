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

class BillingDataDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validateNullFields() {
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("recipientCode", NotBlank.class);
        toCheckMap.put("vatNumber", NotBlank.class);
        toCheckMap.put("taxCode", NotBlank.class);
        toCheckMap.put("zipCode", NotBlank.class);
        toCheckMap.put("digitalAddress", NotBlank.class);
        toCheckMap.put("registeredOffice", NotBlank.class);
        toCheckMap.put("businessName", NotBlank.class);

        BillingDataDto billingDataDto = new BillingDataDto();
        //when
        Set<ConstraintViolation<Object>> violations = validator.validate(billingDataDto);
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
        BillingDataDto billingDataDto = TestUtils.mockInstance(new BillingDataDto());
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(billingDataDto);
        // then
        assertTrue(violations.isEmpty());
    }
}