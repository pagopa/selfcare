package it.pagopa.selfcare.dashboard.model;

import it.pagopa.selfcare.commons.utils.TestUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateUserDtoTest {

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
        toCheckMap.put("productRoles", NotEmpty.class);
        CreateUserDto resource = new CreateUserDto();
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
        // given
        CreateUserDto resource = TestUtils.mockInstance(new CreateUserDto());
        resource.setEmail("email@example.com");
        Set<String> mockProductRoles = Set.of("String");
        resource.setProductRoles(mockProductRoles);
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(resource);
        // then
        assertTrue(violations.isEmpty());
    }


    @Test
    void validate_emailFieldsNotValid() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("email", Email.class);
        CreateUserDto resource = TestUtils.mockInstance(new CreateUserDto());
        Set<String> mockProductRoles = Set.of("String");
        resource.setProductRoles(mockProductRoles);
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

}