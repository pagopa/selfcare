package it.pagopa.selfcare.dashboard.model;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InstitutionResourceTest {

    private Validator validator;
    private static final InstitutionResource INSTITUTION_RESOURCE = TestUtils.mockInstance(new InstitutionResource());

    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validateNullFields() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("id", NotBlank.class);
        toCheckMap.put("externalId", NotBlank.class);
        toCheckMap.put("origin", NotBlank.class);
        toCheckMap.put("originId", NotBlank.class);
        toCheckMap.put("name", NotBlank.class);
        toCheckMap.put("mailAddress", NotBlank.class);
        toCheckMap.put("fiscalCode", NotBlank.class);
        toCheckMap.put("userRole", NotBlank.class);
        toCheckMap.put("status", NotBlank.class);
        toCheckMap.put("address", NotBlank.class);
        toCheckMap.put("zipCode", NotBlank.class);
        toCheckMap.put("geographicTaxonomies", NotNull.class);
        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setInstitutionType(null);
        institutionResource.setRecipientCode(null);
        institutionResource.setVatNumberGroup(null);
        institutionResource.setVatNumber(null);
        institutionResource.setSupportContact(null);

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(institutionResource);
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
        INSTITUTION_RESOURCE.setGeographicTaxonomies(Collections.emptyList());
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(INSTITUTION_RESOURCE);
        // then
        assertTrue(violations.isEmpty());
    }

}