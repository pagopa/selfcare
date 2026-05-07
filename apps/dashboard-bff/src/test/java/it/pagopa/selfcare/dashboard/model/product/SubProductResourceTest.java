package it.pagopa.selfcare.dashboard.model.product;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

class SubProductResourceTest {
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
        toCheckMap.put("id", NotBlank.class);
        toCheckMap.put("title", NotBlank.class);
        toCheckMap.put("productOnBoardingStatus", NotNull.class);
        toCheckMap.put("status", NotNull.class);
        SubProductResource subProductResource = new SubProductResource();
        subProductResource.setId(null);
        subProductResource.setTitle(null);
        subProductResource.setProductOnBoardingStatus(null);
        subProductResource.setStatus(null);
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(subProductResource);
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
        SubProductResource subProductResource = TestUtils.mockInstance(new SubProductResource());
        subProductResource.setLogoBgColor("#FF56E1");
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(subProductResource);
        // then
        assertTrue(violations.isEmpty());
    }
}
