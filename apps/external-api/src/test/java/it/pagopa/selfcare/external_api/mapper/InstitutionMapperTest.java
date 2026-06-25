package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardedProductResponse;
import it.pagopa.selfcare.external_api.model.institution.Institution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class InstitutionMapperTest {

    @InjectMocks
    InstitutionMapperImpl institutionMapperImpl;

    @Test
    void toOnboardingWithDate(){
        InstitutionResponse institutionResponse = new InstitutionResponse();
        OnboardedProductResponse onboardedProductResponse = new OnboardedProductResponse();
        onboardedProductResponse.setProductId("productId");
        onboardedProductResponse.setStatus(OnboardedProductResponse.StatusEnum.ACTIVE);
        onboardedProductResponse.setCreatedAt(LocalDateTime.now());
        institutionResponse.setOnboarding(List.of(onboardedProductResponse));
        Institution institution = institutionMapperImpl.toInstitution(institutionResponse);
        Assertions.assertNotNull(institution);
        Assertions.assertNotNull(institution.getOnboarding());

    }

}
