package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomy;
import it.pagopa.selfcare.onboarding.client.model.InstitutionOnboardingData;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.controller.request.CompanyOnboardingDto;
import it.pagopa.selfcare.onboarding.controller.request.OnboardingProductDto;
import it.pagopa.selfcare.onboarding.controller.response.GeographicTaxonomyResource;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionOnboardingInfoResource;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionControllerTest {

    @Mock
    InstitutionService institutionService;
    @Mock
    OnboardingMapper onboardingMapper;
    @Mock
    InstitutionMapper institutionMapper;
    @Mock
    UserMapper userMapper;

    @InjectMocks
    InstitutionController institutionController;

    @Test
    void onboarding_callsServiceAndReturnsCreated() {
        OnboardingProductDto request = new OnboardingProductDto();
        OnboardingData onboardingData = new OnboardingData();
        when(onboardingMapper.toEntity(request)).thenReturn(onboardingData);

        Response response = institutionController.onboarding(request);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(institutionService).onboardingProduct(onboardingData);
    }

    @Test
    void onboardingCompany_callsServiceAndReturnsCreated() {
        CompanyOnboardingDto request = new CompanyOnboardingDto();
        OnboardingData onboardingData = new OnboardingData();
        when(onboardingMapper.toEntity(request)).thenReturn(onboardingData);

        Response response = institutionController.onboarding(request);

        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(institutionService).onboardingProduct(onboardingData);
    }

    @Test
    void getInstitutionOnboardingInfoById_mapsResult() {
        InstitutionOnboardingData data = new InstitutionOnboardingData();
        InstitutionOnboardingInfoResource expected = new InstitutionOnboardingInfoResource();
        when(institutionService.getInstitutionOnboardingDataById("instId", "prodId")).thenReturn(data);
        when(institutionMapper.toResource(data)).thenReturn(expected);

        InstitutionOnboardingInfoResource result = institutionController.getInstitutionOnboardingInfoById("instId", "prodId");

        assertSame(expected, result);
    }

    @Test
    void getInstitutionGeographicTaxonomy_mapsList() {
        GeographicTaxonomy taxonomy = new GeographicTaxonomy();
        GeographicTaxonomyResource expected = new GeographicTaxonomyResource();
        when(institutionService.getGeographicTaxonomyList("extId")).thenReturn(List.of(taxonomy));
        when(institutionMapper.toResource(any(GeographicTaxonomy.class))).thenReturn(expected);

        List<GeographicTaxonomyResource> result = institutionController.getInstitutionGeographicTaxonomy("extId");

        assertEquals(1, result.size());
        assertSame(expected, result.get(0));
    }

    @Test
    void verifyOnboarding_callsService() {
        institutionController.verifyOnboarding("extId", "prodId");

        verify(institutionService).verifyOnboarding("extId", "prodId");
    }
}
