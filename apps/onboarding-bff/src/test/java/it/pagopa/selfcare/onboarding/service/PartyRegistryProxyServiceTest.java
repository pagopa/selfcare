package it.pagopa.selfcare.onboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.PartyRegistryProxyRestClient;
import it.pagopa.selfcare.onboarding.client.model.AooResponse;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionByLegalTaxIdRequest;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.client.model.UoResponse;
import it.pagopa.selfcare.onboarding.client.model.ProxyInstitutionResponse;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResponse;
import it.pagopa.selfcare.onboarding.mapper.RegistryProxyMapper;
import java.util.List;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartyRegistryProxyServiceTest {

    @InjectMocks
    private PartyRegistryProxyService partyRegistryProxyService;

    @Mock
    private PartyRegistryProxyRestClient restClient;

    @Spy
    private RegistryProxyMapper proxyMapper = Mappers.getMapper(RegistryProxyMapper.class);

    @Test
    void findIpaInstitution_mapsResponseAndPreservesCategory() {
        var response = new ProxyInstitutionResponse();
        response.setTaxCode("00000000000");
        response.setCategory("L6");
        when(restClient.findIpaInstitutionByTaxCode("00000000000", "L6")).thenReturn(response);

        var result = partyRegistryProxyService.findIpaInstitutionByTaxCode("00000000000", "L6");

        assertEquals("00000000000", result.getTaxCode());
        assertEquals("L6", result.getCategory());
        verify(restClient).findIpaInstitutionByTaxCode("00000000000", "L6");
    }

    @Test
    void searchIpaInstitutions_mapsResponseAndForwardsEveryFilter() {
        var response = new IpaInstitutionsSearchResponse();
        response.setItems(List.of());
        response.setCount(0L);
        when(restClient.searchIpaInstitutions("Roma", "L6", 2, 50)).thenReturn(response);

        var result = partyRegistryProxyService.searchIpaInstitutions("Roma", "L6", 2, 50);

        assertEquals(0L, result.getCount());
        assertEquals(List.of(), result.getItems());
        verify(restClient).searchIpaInstitutions("Roma", "L6", 2, 50);
    }

    @Test
    void getInstitutionsByUserFiscalCode_buildsExpectedRequest() {
        InstitutionInfoIC expected = new InstitutionInfoIC();
        when(restClient.getInstitutionsByUserLegalTaxId(any())).thenReturn(expected);

        InstitutionInfoIC result = partyRegistryProxyService.getInstitutionsByUserFiscalCode("AAAABBBB");

        assertSame(expected, result);
        ArgumentCaptor<InstitutionByLegalTaxIdRequest> captor = ArgumentCaptor.forClass(InstitutionByLegalTaxIdRequest.class);
        verify(restClient).getInstitutionsByUserLegalTaxId(captor.capture());
        assertEquals("AAAABBBB", captor.getValue().getFilter().getLegalTaxId());
    }

    @Test
    void getInstitutionsByUserFiscalCode_whenBlankTaxCode_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> partyRegistryProxyService.getInstitutionsByUserFiscalCode("  "));
        assertEquals("An user's fiscal code is required", ex.getMessage());
    }

    @Test
    void getAooById_delegatesToRestClient() {
        AooResponse expected = new AooResponse();
        expected.setCodAoo("AOO1");
        when(restClient.getAooById("AOO1")).thenReturn(expected);

        AooResponse result = partyRegistryProxyService.getAooById("AOO1");

        assertSame(expected, result);
    }

    @Test
    void getUoById_delegatesToRestClient() {
        UoResponse expected = new UoResponse();
        expected.setUniUoCode("UO1");
        when(restClient.getUoById("UO1")).thenReturn(expected);

        UoResponse result = partyRegistryProxyService.getUoById("UO1");

        assertSame(expected, result);
    }

    @Test
    void getExtById_delegatesToRestClient() {
        GeographicTaxonomiesResponse expected = new GeographicTaxonomiesResponse();
        expected.setGeotaxId("GEO1");
        when(restClient.getExtByCode("GEO1")).thenReturn(expected);

        GeographicTaxonomiesResponse result = partyRegistryProxyService.getExtById("GEO1");

        assertSame(expected, result);
    }
}
