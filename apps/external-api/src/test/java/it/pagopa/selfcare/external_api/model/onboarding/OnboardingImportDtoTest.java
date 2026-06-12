package it.pagopa.selfcare.external_api.model.onboarding;

import it.pagopa.selfcare.external_api.model.user.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingImportDtoTest {

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
        toCheckMap.put("users", NotEmpty.class);
        toCheckMap.put("importContract", NotNull.class);
        OnboardingImportDto model = new OnboardingImportDto();
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(model);
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
        OnboardingImportDto model = mockInstance(new OnboardingImportDto());
        UserDto userDto = mockInstance(new UserDto());
        userDto.setEmail("email@example.com");
        ImportContractDto importContractDto = mockInstance(new ImportContractDto());
        model.setUsers(List.of(userDto));
        model.setImportContract(importContractDto);
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(model);
        // then
        assertTrue(violations.isEmpty());
    }
}
