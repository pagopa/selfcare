package it.pagopa.selfcare.external_api.model.onboarding;

import it.pagopa.selfcare.commons.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportContractDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validateNullFields() {
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("fileName", NotBlank.class);
        toCheckMap.put("filePath", NotBlank.class);
        toCheckMap.put("contractType", NotBlank.class);
        toCheckMap.put("onboardingDate", NotNull.class);

        ImportContractDto importContractDto = new ImportContractDto();
        //when
        Set<ConstraintViolation<Object>> violations = validator.validate(importContractDto);
        // then
        List<ConstraintViolation<Object>> filteredViolations = violations.stream()
                .filter(violation -> {
                    Class<? extends Annotation> annotationToCheck = toCheckMap.get(violation.getPropertyPath().toString());
                    return !violation.getConstraintDescriptor().getAnnotation().annotationType().equals(annotationToCheck);
                })
                .collect(Collectors.toList());
        assertTrue(filteredViolations.isEmpty());
    }

    @Test
    void validateNotNullFields() {
        // given
        ImportContractDto importContractDto = TestUtils.mockInstance(new ImportContractDto());
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(importContractDto);
        // then
        assertTrue(violations.isEmpty());
    }
}
