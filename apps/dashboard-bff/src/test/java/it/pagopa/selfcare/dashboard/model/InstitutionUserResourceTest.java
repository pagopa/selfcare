package it.pagopa.selfcare.dashboard.model;

import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.dashboard.model.product.ProductInfoResource;
import it.pagopa.selfcare.dashboard.model.product.ProductRoleInfoResource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Email;
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

class InstitutionUserResourceTest {

    private Validator validator;


    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }


    @Test
    void validateNullFields_InstitutionUserResource() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("id", NotNull.class);
        toCheckMap.put("name", NotBlank.class);
        toCheckMap.put("surname", NotBlank.class);
        toCheckMap.put("email", NotBlank.class);
        toCheckMap.put("role", NotNull.class);
        toCheckMap.put("status", NotBlank.class);
        toCheckMap.put("products", NotNull.class);
        InstitutionUserResource institutionResource = new InstitutionUserResource();
        institutionResource.setId(null);
        institutionResource.setName(null);
        institutionResource.setSurname(null);
        institutionResource.setEmail(null);
        institutionResource.setRole(null);
        institutionResource.setStatus(null);
        institutionResource.setProducts(null);
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
    void validateNotNullFields_InstitutionUserResource() {
        // given
        InstitutionUserResource institutionUserResource = TestUtils.mockInstance(new InstitutionUserResource());
        institutionUserResource.setProducts(Collections.emptyList());
        institutionUserResource.setEmail("email@example.com");
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(institutionUserResource);
        // then
        assertTrue(violations.isEmpty());
    }


    @Test
    void validate_emailFieldsNotValid_InstitutionUserResource() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("email", Email.class);
        InstitutionUserResource institutionUserResource = TestUtils.mockInstance(new InstitutionUserResource());
        institutionUserResource.setProducts(Collections.emptyList());
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(institutionUserResource);
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
    void validateNullFields_ProductInfo() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("id", NotBlank.class);
        toCheckMap.put("title", NotBlank.class);
        toCheckMap.put("roleInfos", NotNull.class);
        ProductInfoResource productInfo = new ProductInfoResource();
        productInfo.setId(null);
        productInfo.setTitle(null);
        productInfo.setRoleInfos(null);

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(productInfo);
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
    void validateNotNullFields_ProductInfo() {
        // given
        ProductInfoResource productInfo = TestUtils.mockInstance(new ProductInfoResource());
        productInfo.setRoleInfos(List.of(TestUtils.mockInstance(new ProductRoleInfoResource())));
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(productInfo);
        // then
        assertTrue(violations.isEmpty());
    }

}