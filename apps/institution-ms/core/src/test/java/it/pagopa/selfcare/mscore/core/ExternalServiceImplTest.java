package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.exception.InvalidRequestException;
import it.pagopa.selfcare.mscore.model.institution.GeographicTaxonomies;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalServiceImplTest {

    @InjectMocks
    private ExternalServiceImpl externalServiceImpl;

    @Mock
    private InstitutionService institutionService;


    /**
     * Method under test: {@link ExternalServiceImpl#getInstitutionByExternalId(String)}
     */
    @Test
    void testGetInstitutionByExternalId() {
        Institution institution = new Institution();
        when(institutionService.retrieveInstitutionByExternalId(any())).thenReturn(institution);
        assertSame(institution, externalServiceImpl.getInstitutionByExternalId("42"));
        verify(institutionService).retrieveInstitutionByExternalId(any());
    }

    /**
     * Method under test: {@link ExternalServiceImpl#getInstitutionByExternalId(String)}
     */
    @Test
    void testGetInstitutionByExternalId2() {
        Institution institution = new Institution();
        when(institutionService.retrieveInstitutionByExternalId(any())).thenReturn(institution);
        assertSame(institution, externalServiceImpl.getInstitutionByExternalId("42"));
        verify(institutionService).retrieveInstitutionByExternalId(any());
    }

    /**
     * Method under test: {@link ExternalServiceImpl#getInstitutionByExternalId(String)}
     */
    @Test
    void testGetInstitutionByExternalId3() {
        when(institutionService.retrieveInstitutionByExternalId(any()))
                .thenThrow(new InvalidRequestException("An error occurred", "Code"));
        assertThrows(InvalidRequestException.class, () -> externalServiceImpl.getInstitutionByExternalId("42"));
        verify(institutionService).retrieveInstitutionByExternalId(any());
    }


    /**
     * Method under test: {@link ExternalServiceImpl#retrieveInstitutionProduct(String, String)}
     */
    @Test
    void testRetrieveInstitutionProduct() {
        Institution institution = new Institution();
        when(institutionService.retrieveInstitutionProduct(any(), any())).thenReturn(institution);
        assertSame(institution, externalServiceImpl.retrieveInstitutionProduct("42", "42"));
        verify(institutionService).retrieveInstitutionProduct(any(), any());
    }

    /**
     * Method under test: {@link ExternalServiceImpl#retrieveInstitutionProduct(String, String)}
     */
    @Test
    void testRetrieveInstitutionProduct2() {
        Institution institution = new Institution();
        when(institutionService.retrieveInstitutionProduct(any(), any())).thenReturn(institution);
        assertSame(institution, externalServiceImpl.retrieveInstitutionProduct("42", "42"));
        verify(institutionService).retrieveInstitutionProduct(any(), any());
    }

    /**
     * Method under test: {@link ExternalServiceImpl#retrieveInstitutionProduct(String, String)}
     */
    @Test
    void testRetrieveInstitutionProduct3() {
        when(institutionService.retrieveInstitutionProduct(any(), any()))
                .thenThrow(new InvalidRequestException("An error occurred", "Code"));
        assertThrows(InvalidRequestException.class, () -> externalServiceImpl.retrieveInstitutionProduct("42", "42"));
        verify(institutionService).retrieveInstitutionProduct(any(), any());
    }

    /**
     * Method under test: {@link ExternalServiceImpl#retrieveInstitutionProductsByExternalId(String, List)}
     */
    @Test
    void testRetrieveInstitutionProductsByExternalId() {
        when(institutionService.retrieveInstitutionByExternalId(any())).thenReturn(new Institution());
        ArrayList<Onboarding> onboardingList = new ArrayList<>();
        when(institutionService.retrieveInstitutionProducts(any(), any()))
                .thenReturn(onboardingList);
        List<Onboarding> actualRetrieveInstitutionProductsByExternalIdResult = externalServiceImpl
                .retrieveInstitutionProductsByExternalId("42", new ArrayList<>());
        assertSame(onboardingList, actualRetrieveInstitutionProductsByExternalIdResult);
        assertTrue(actualRetrieveInstitutionProductsByExternalIdResult.isEmpty());
        verify(institutionService).retrieveInstitutionByExternalId(any());
        verify(institutionService).retrieveInstitutionProducts(any(), any());
    }

    /**
     * Method under test: {@link ExternalServiceImpl#retrieveInstitutionProductsByExternalId(String, List)}
     */
    @Test
    void testRetrieveInstitutionProductsByExternalId2() {
        when(institutionService.retrieveInstitutionByExternalId(any()))
                .thenThrow(new InvalidRequestException("An error occurred", "Code"));
        List<RelationshipState> list = new ArrayList<>();
        assertThrows(InvalidRequestException.class,
                () -> externalServiceImpl.retrieveInstitutionProductsByExternalId("42", list));
        verify(institutionService).retrieveInstitutionByExternalId(any());
    }

    /**
     * Method under test: {@link ExternalServiceImpl#retrieveInstitutionByIds(List)}
     */
    @Test
    void testRetrieveInstitutionByIds() {
        ArrayList<Institution> institutionList = new ArrayList<>();
        when(institutionService.retrieveInstitutionByIds(any())).thenReturn(institutionList);
        List<Institution> actualRetrieveInstitutionByIdsResult = externalServiceImpl
                .retrieveInstitutionByIds(new ArrayList<>());
        assertSame(institutionList, actualRetrieveInstitutionByIdsResult);
        assertTrue(actualRetrieveInstitutionByIdsResult.isEmpty());
        verify(institutionService).retrieveInstitutionByIds(any());
    }

    /**
     * Method under test: {@link ExternalServiceImpl#retrieveInstitutionByIds(List)}
     */
    @Test
    void testRetrieveInstitutionByIds2() {
        when(institutionService.retrieveInstitutionByIds(any()))
                .thenThrow(new InvalidRequestException("An error occurred", "Code"));
        List<String> ids = new ArrayList<>();
        assertThrows(InvalidRequestException.class,
                () -> externalServiceImpl.retrieveInstitutionByIds(ids));
        verify(institutionService).retrieveInstitutionByIds(any());
    }

    /**
     * Method under test: {@link ExternalServiceImpl#retrieveInstitutionGeoTaxonomiesByExternalId(String)}
     */
    @Test
    void testRetrieveInstitutionGeoTaxonomiesByExternalId() {
        when(institutionService.retrieveInstitutionByExternalId(any())).thenReturn(new Institution());
        ArrayList<GeographicTaxonomies> geographicTaxonomiesList = new ArrayList<>();
        when(institutionService.retrieveInstitutionGeoTaxonomies(any()))
                .thenReturn(geographicTaxonomiesList);
        List<GeographicTaxonomies> actualRetrieveInstitutionGeoTaxonomiesByExternalIdResult = externalServiceImpl
                .retrieveInstitutionGeoTaxonomiesByExternalId("42");
        assertSame(geographicTaxonomiesList, actualRetrieveInstitutionGeoTaxonomiesByExternalIdResult);
        assertTrue(actualRetrieveInstitutionGeoTaxonomiesByExternalIdResult.isEmpty());
        verify(institutionService).retrieveInstitutionByExternalId(any());
        verify(institutionService).retrieveInstitutionGeoTaxonomies(any());
    }
}
