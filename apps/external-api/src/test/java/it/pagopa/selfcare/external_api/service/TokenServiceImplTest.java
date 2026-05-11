package it.pagopa.selfcare.external_api.service;

import it.pagopa.selfcare.external_api.client.MsOnboardingControllerApi;
import it.pagopa.selfcare.external_api.mapper.TokenMapperImpl;
import it.pagopa.selfcare.external_api.model.token.TokenOnboardedUsers;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGet;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class TokenServiceImplTest extends BaseServiceTestUtils {

    @InjectMocks
    private TokenServiceImpl tokenService;

    @Mock
    private MsOnboardingControllerApi onboardingControllerApi;

    @Spy
    private TokenMapperImpl tokenMapper;


    @BeforeEach
    public void setUp() {
        super.setUp();
    }

    @Test
    void findByProductId() {
        OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
        OnboardingGet onboardingGet = new OnboardingGet();
        onboardingGet.setProductId("id");
        onboardingGetResponse.setItems(List.of(onboardingGet));
        when(onboardingControllerApi._getOnboardingWithFilter(
                null,
                null,
                null,
                1,
                "id",
                null,
                10,
                null,
                OnboardingStatus.COMPLETED,
                null,
                null,
                null,
                null))
            .thenReturn(ResponseEntity.ok(onboardingGetResponse));
        List<TokenOnboardedUsers> tokens = this.tokenService.findByProductId("id", 1, 10, OnboardingStatus.COMPLETED.name());
        Assertions.assertEquals(1, tokens.size());
    }

    @Test
    void findByProductIdEmptyList() {
        OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
        onboardingGetResponse.setItems(Collections.emptyList());
        when(onboardingControllerApi._getOnboardingWithFilter(null, null, null, 1, "id", null, 10, null, null, null, null, null, null))
                .thenReturn(ResponseEntity.ok(onboardingGetResponse));
        List<TokenOnboardedUsers> tokens = this.tokenService.findByProductId("id", 1, 10, null);
        Assertions.assertEquals(0, tokens.size());
    }
}
