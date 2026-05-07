package it.pagopa.selfcare.dashboard.model.onboarding;

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

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnboardingRequestResourceTest {

    private Validator validator;


    @BeforeEach
    void setUp() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validateNullFields_OnboardingRequestResource() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("status", NotNull.class);
        toCheckMap.put("institutionInfo", NotNull.class);
        toCheckMap.put("manager", NotNull.class);
        toCheckMap.put("admins", NotNull.class);

        OnboardingRequestResource resource = new OnboardingRequestResource();

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(resource);
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
    void validateNullFields_UserInfo() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("id", NotNull.class);
        toCheckMap.put("name", NotBlank.class);
        toCheckMap.put("surname", NotBlank.class);
        toCheckMap.put("email", NotBlank.class);
        toCheckMap.put("fiscalCode", NotBlank.class);

        OnboardingRequestResource.UserInfo resource = new OnboardingRequestResource.UserInfo();

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(resource);
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
    void validateNullFields_InstitutionInfo() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("id", NotBlank.class);
        toCheckMap.put("name", NotBlank.class);
        toCheckMap.put("institutionType", NotNull.class);
        toCheckMap.put("address", NotBlank.class);
        toCheckMap.put("zipCode", NotBlank.class);
        toCheckMap.put("mailAddress", NotBlank.class);
        toCheckMap.put("fiscalCode", NotBlank.class);
        toCheckMap.put("vatNumber", NotBlank.class);
        toCheckMap.put("recipientCode", NotBlank.class);

        OnboardingRequestResource.InstitutionInfo resource = new OnboardingRequestResource.InstitutionInfo();

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(resource);
        // then
        List<ConstraintViolation<Object>> filteredViolations = violations.stream()
                .filter(violation -> {
                    Class<? extends Annotation> annotationToCheck = toCheckMap.get(violation.getPropertyPath().toString());
                    return !violation.getConstraintDescriptor().getAnnotation().annotationType().equals(annotationToCheck);
                })
                .toList();
        assertTrue(filteredViolations.isEmpty(), filteredViolations.toString());
    }


    @Test
    void validateNullFields_PspData() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("businessRegisterNumber", NotBlank.class);
        toCheckMap.put("legalRegisterName", NotBlank.class);
        toCheckMap.put("legalRegisterNumber", NotBlank.class);
        toCheckMap.put("abiCode", NotBlank.class);
        toCheckMap.put("vatNumberGroup", NotNull.class);

        OnboardingRequestResource.InstitutionInfo.PspData resource = new OnboardingRequestResource.InstitutionInfo.PspData();

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(resource);
        // then
        List<ConstraintViolation<Object>> filteredViolations = violations.stream()
                .filter(violation -> {
                    Class<? extends Annotation> annotationToCheck = toCheckMap.get(violation.getPropertyPath().toString());
                    return !violation.getConstraintDescriptor().getAnnotation().annotationType().equals(annotationToCheck);
                })
                .toList();
        assertTrue(filteredViolations.isEmpty(), filteredViolations.toString());
    }


    @Test
    void validateNullFields_DpoData() {
        // given
        HashMap<String, Class<? extends Annotation>> toCheckMap = new HashMap<>();
        toCheckMap.put("address", NotBlank.class);
        toCheckMap.put("pec", NotBlank.class);
        toCheckMap.put("email", NotBlank.class);

        OnboardingRequestResource.InstitutionInfo.DpoData resource = new OnboardingRequestResource.InstitutionInfo.DpoData();

        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(resource);
        // then
        List<ConstraintViolation<Object>> filteredViolations = violations.stream()
                .filter(violation -> {
                    Class<? extends Annotation> annotationToCheck = toCheckMap.get(violation.getPropertyPath().toString());
                    return !violation.getConstraintDescriptor().getAnnotation().annotationType().equals(annotationToCheck);
                })
                .toList();
        assertTrue(filteredViolations.isEmpty(), filteredViolations.toString());
    }


    @Test
    void validateNotNullFields() {
        // given
        OnboardingRequestResource resource = mockInstance(new OnboardingRequestResource());
        resource.getInstitutionInfo().setMailAddress("email@example.com");
        resource.getInstitutionInfo().getDpoData().setEmail("email@example.com");
        resource.getInstitutionInfo().getDpoData().setPec("email@example.com");
        resource.getManager().setEmail("email@example.com");
        resource.setAdmins(Collections.emptyList());
        // when
        Set<ConstraintViolation<Object>> violations = validator.validate(resource);
        // then
        assertTrue(violations.isEmpty(), violations.toString());
    }


}