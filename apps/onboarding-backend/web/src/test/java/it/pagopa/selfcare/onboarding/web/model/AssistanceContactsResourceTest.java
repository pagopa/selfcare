package it.pagopa.selfcare.onboarding.web.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.selfcare.commons.utils.TestUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssistanceContactsResourceTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validateNotNullFields() {
        // given
        AssistanceContactsResource assistanceContactsResource = TestUtils.mockInstance(new AssistanceContactsResource());
        assistanceContactsResource.setSupportEmail("mail@example.com");
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(assistanceContactsResource);
        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    void validate_emailFieldsNotValid() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("supportEmail", Email.class);
        AssistanceContactsResource model = TestUtils.mockInstance(new AssistanceContactsResource());
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
