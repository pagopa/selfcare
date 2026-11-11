package it.pagopa.selfcare.onboarding.web.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingProductDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<ConstraintViolation<OnboardingProductDto>> rtpViolations(OnboardingProductDto dto) {
        return validator.validate(dto).stream()
                .filter(v -> v.getConstraintDescriptor().getAnnotation() instanceof AssertTrue)
                .collect(java.util.stream.Collectors.toSet());
    }

    private OnboardingProductDto dto(String productId, List<RtpServiceTypeDto> types) {
        OnboardingProductDto dto = new OnboardingProductDto();
        dto.setProductId(productId);
        dto.setRtpServiceTypes(types);
        return dto;
    }

    @Test
    void rtpServiceTypesNull_prodRtp_isInvalid() {
        Set<ConstraintViolation<OnboardingProductDto>> violations = rtpViolations(dto("prod-rtp", null));

        assertEquals(1, violations.size());
        assertEquals("rtpServiceTypes is required and must not be empty for prod-rtp",
                violations.iterator().next().getMessage());
    }

    @Test
    void rtpServiceTypesEmpty_prodRtp_isInvalid() {
        assertEquals(1, rtpViolations(dto("prod-rtp", List.of())).size());
    }

    @Test
    void rtpServiceTypesPresent_prodRtp_isValid() {
        assertTrue(rtpViolations(dto("prod-rtp", List.of(RtpServiceTypeDto.RTP))).isEmpty());
    }

    @Test
    void rtpServiceTypesMissing_otherProduct_isValid() {
        assertTrue(rtpViolations(dto("prod-pagopa", null)).isEmpty());
        assertTrue(rtpViolations(dto(null, null)).isEmpty());
    }
}
