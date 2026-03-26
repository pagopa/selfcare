package it.pagopa.selfcare.onboarding.web.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import it.pagopa.selfcare.commons.utils.TestUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeographicTaxonomyListResourceTest {
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
        GeographicTaxonomyListResource geographicTaxonomyResource = new GeographicTaxonomyListResource();
        toCheckMap.put("geographicTaxonomies", NotNull.class);
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(geographicTaxonomyResource);
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
        GeographicTaxonomyListResource geographicTaxonomyResource = TestUtils.mockInstance(new GeographicTaxonomyListResource());
        geographicTaxonomyResource.setGeographicTaxonomies(List.of(TestUtils.mockInstance(new GeographicTaxonomyResource())));
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(geographicTaxonomyResource);
        // then
        assertTrue(violations.isEmpty());
    }

}
