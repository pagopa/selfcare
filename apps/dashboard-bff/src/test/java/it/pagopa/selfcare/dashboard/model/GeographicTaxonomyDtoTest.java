package it.pagopa.selfcare.dashboard.model;

import it.pagopa.selfcare.commons.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GeographicTaxonomyDtoTest {

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
        toCheckMap.put("code", NotBlank.class);
        toCheckMap.put("desc", NotBlank.class);
        GeographicTaxonomyDto geographicTaxonomyDto = new GeographicTaxonomyDto();

        //when
        Set<ConstraintViolation<Object>> violations = validator.validate(geographicTaxonomyDto);
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
        GeographicTaxonomyDto model = TestUtils.mockInstance(new GeographicTaxonomyDto());
        ;
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(model);
        // then
        assertTrue(violations.isEmpty());
    }

}