package it.pagopa.selfcare.external_api.model.institutions;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.external_api.model.institution.GeographicTaxonomyResource;
import it.pagopa.selfcare.external_api.model.institution.InstitutionDetailResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;

;

class InstitutionDetailResourceTest {
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
        toCheckMap.put("id", NotBlank.class);
        toCheckMap.put("description", NotBlank.class);
        toCheckMap.put("externalId", NotBlank.class);
        toCheckMap.put("originId", NotBlank.class);
        toCheckMap.put("digitalAddress", NotBlank.class);
        toCheckMap.put("address", NotBlank.class);
        toCheckMap.put("zipCode", NotBlank.class);
        toCheckMap.put("taxCode", NotBlank.class);
        toCheckMap.put("geographicTaxonomies", NotNull.class);

        InstitutionDetailResource institutionResource = new InstitutionDetailResource();

        Set<ConstraintViolation<Object>> violations = validator.validate(institutionResource);
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
        InstitutionDetailResource institutionResource = mockInstance(new InstitutionDetailResource());
        institutionResource.setGeographicTaxonomies(List.of(mockInstance(new GeographicTaxonomyResource())));
        institutionResource.setInstitutionType(InstitutionType.PT);
        institutionResource.setSupportEmail("email@email.com");

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(institutionResource);
        // then
        assertTrue(violations.isEmpty());
    }
}
