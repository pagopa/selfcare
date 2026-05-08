package it.pagopa.selfcare.dashboard.model.user_groups;

import it.pagopa.selfcare.commons.utils.TestUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserGroupPlainResourceTest {
    private Validator validator;


    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validateNullFields() {
        //given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("id", NotBlank.class);
        toCheckMap.put("institutionId", NotBlank.class);
        toCheckMap.put("productId", NotBlank.class);
        toCheckMap.put("name", NotBlank.class);
        toCheckMap.put("description", NotBlank.class);
        toCheckMap.put("membersCount", NotNull.class);
        toCheckMap.put("status", NotNull.class);
        UserGroupPlainResource resource = new UserGroupPlainResource();
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(resource);
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
        //given
        UserGroupPlainResource resource = TestUtils.mockInstance(new UserGroupPlainResource());
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(resource);
        // then
        assertTrue(violations.isEmpty());
    }

}
