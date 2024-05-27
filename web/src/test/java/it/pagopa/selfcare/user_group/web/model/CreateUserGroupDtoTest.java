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
import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateUserGroupDtoTest {
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
        toCheckMap.put("institutionId", NotBlank.class);
        toCheckMap.put("productId", NotBlank.class);
        toCheckMap.put("name", NotBlank.class);
        toCheckMap.put("description", NotBlank.class);
        toCheckMap.put("status", NotNull.class);
        toCheckMap.put("members", NotEmpty.class);
        CreateUserGroupDto createUserGroupDto = new CreateUserGroupDto();
        createUserGroupDto.setDescription(null);
        createUserGroupDto.setMembers(null);
        createUserGroupDto.setName(null);
        createUserGroupDto.setStatus(null);
        createUserGroupDto.setProductId(null);
        createUserGroupDto.setInstitutionId(null);
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(createUserGroupDto);
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
        CreateUserGroupDto userGroupDto = TestUtils.mockInstance(new CreateUserGroupDto());
        userGroupDto.setMembers(Set.of(UUID.randomUUID()));
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(userGroupDto);
        // then
        assertTrue(violations.isEmpty());
    }
}