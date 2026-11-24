package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OnboardingPspRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    static Stream<Arguments> rtpServiceTypes() {
        return Stream.of("prod-rtp", "prod-pagopa")
                .flatMap(productId -> Stream.of(
                        Arguments.of(productId, null, !"prod-rtp".equals(productId)),
                        Arguments.of(productId, List.of(), !"prod-rtp".equals(productId)),
                        Arguments.of(productId, List.of(RtpServiceTypeRequest.RTP), true)));
    }

    @ParameterizedTest
    @MethodSource("rtpServiceTypes")
    void shouldRequireServiceTypesOnlyForRtp(String productId, List<RtpServiceTypeRequest> serviceTypes,
                                             boolean valid) {
        OnboardingPspRequest request = new OnboardingPspRequest();
        request.setProductId(productId);
        request.setRtpServiceTypes(serviceTypes);

        assertEquals(valid, VALIDATOR_FACTORY.getValidator()
                .validateProperty(request, "rtpServiceTypesValid").isEmpty());
    }
}
