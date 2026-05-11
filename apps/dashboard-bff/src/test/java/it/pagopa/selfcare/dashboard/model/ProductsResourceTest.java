package it.pagopa.selfcare.dashboard.model;

import it.pagopa.selfcare.commons.utils.TestUtils;
import it.pagopa.selfcare.dashboard.model.product.ProductsResource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductsResourceTest {

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
        toCheckMap.put("logo", NotBlank.class);
        toCheckMap.put("imageUrl", NotBlank.class);
        toCheckMap.put("title", NotBlank.class);
        toCheckMap.put("description", NotBlank.class);
        toCheckMap.put("urlBO", NotBlank.class);
        toCheckMap.put("productOnBoardingStatus", NotNull.class);
        toCheckMap.put("status", NotNull.class);
        ProductsResource productsResource = new ProductsResource();
        productsResource.setId(null);
        productsResource.setLogo(null);
        productsResource.setImageUrl(null);
        productsResource.setTitle(null);
        productsResource.setDescription(null);
        productsResource.setLogoBgColor(null);
        productsResource.setUrlBO(null);
        productsResource.setStatus(null);
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(productsResource);
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
        ProductsResource productsResource = TestUtils.mockInstance(new ProductsResource());
        productsResource.setLogoBgColor("#FF56E1");
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(productsResource);
        // then
        assertTrue(violations.isEmpty());
    }

}