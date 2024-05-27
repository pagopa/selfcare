package it.pagopa.selfcare.user_group.web.model;

import it.pagopa.selfcare.commons.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateUserGroupDtoTest {
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
        toCheckMap.put("name", NotBlank.class);
        toCheckMap.put("description", NotBlank.class);
        toCheckMap.put("members", NotEmpty.class);
        UpdateUserGroupDto updateUserGroupDto = new UpdateUserGroupDto();
        updateUserGroupDto.setDescription(null);
        updateUserGroupDto.setMembers(null);
        updateUserGroupDto.setName(null);

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(updateUserGroupDto);
        // then
        assertFalse(violations.isEmpty());
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
        UpdateUserGroupDto userGroupDto = TestUtils.mockInstance(new UpdateUserGroupDto());
        userGroupDto.setMembers(Set.of(UUID.randomUUID()));
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(userGroupDto);
        // then
        assertTrue(violations.isEmpty());
    }

}