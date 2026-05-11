package it.pagopa.selfcare.external_api.model.product;

import it.pagopa.selfcare.commons.utils.TestUtils;
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

class ProductRoleInfoTest {
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
        toCheckMap.put("multiroleAllowed", NotNull.class);
        toCheckMap.put("roles", NotEmpty.class);

        ProductRoleInfo productRoleInfo = new ProductRoleInfo();

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(productRoleInfo);
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
        ProductRoleInfo productRoleInfo = TestUtils.mockInstance(new ProductRoleInfo());

        productRoleInfo.setRoles(List.of(mockInstance(new ProductRoleInfo.ProductRole())));
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(productRoleInfo);
        // then
        assertTrue(violations.isEmpty());
    }
}
