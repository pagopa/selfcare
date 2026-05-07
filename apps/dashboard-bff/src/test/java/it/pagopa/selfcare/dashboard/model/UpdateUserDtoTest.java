package it.pagopa.selfcare.dashboard.model;

import it.pagopa.selfcare.commons.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateUserDtoTest {

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

        UpdateUserDto userDto = new UpdateUserDto();
        userDto.setEmail(null);
        userDto.setName(null);
        userDto.setSurname(null);
        //when
        Set<ConstraintViolation<Object>> violations = validator.validate(userDto);
        //then
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
        UpdateUserDto model = TestUtils.mockInstance(new UpdateUserDto());
        model.setEmail("email@example.com");
        model.setMobilePhone("1234567890");
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(model);
        // then
        assertTrue(violations.isEmpty());
    }


    @Test
    void validate_emailFieldsNotValid() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("email", Email.class);
        UpdateUserDto model = TestUtils.mockInstance(new UpdateUserDto());
        model.setMobilePhone("1234567890");
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

}