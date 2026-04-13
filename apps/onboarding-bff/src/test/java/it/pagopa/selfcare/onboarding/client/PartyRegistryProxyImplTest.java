package it.pagopa.selfcare.onboarding.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.AooResponse;
import it.pagopa.selfcare.onboarding.client.model.GeographicTaxonomiesResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionByLegalTaxIdRequest;
import it.pagopa.selfcare.onboarding.client.model.InstitutionInfoIC;
import it.pagopa.selfcare.onboarding.client.model.UoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PartyRegistryProxyImplTest {

    @InjectMocks
    private PartyRegistryProxyClient partyRegistryProxyClient;

    @Mock
    private PartyRegistryProxyRestClient restClient;

    @Test
    void getInstitutionsByUserFiscalCode_buildsExpectedRequest() {
        InstitutionInfoIC expected = new InstitutionInfoIC();
        when(restClient.getInstitutionsByUserLegalTaxId(any())).thenReturn(expected);

        InstitutionInfoIC result = partyRegistryProxyClient.getInstitutionsByUserFiscalCode("AAAABBBB");

        assertSame(expected, result);
        ArgumentCaptor<InstitutionByLegalTaxIdRequest> captor = ArgumentCaptor.forClass(InstitutionByLegalTaxIdRequest.class);
        verify(restClient).getInstitutionsByUserLegalTaxId(captor.capture());
        assertEquals("AAAABBBB", captor.getValue().getFilter().getLegalTaxId());
    }

    @Test
    void getInstitutionsByUserFiscalCode_whenBlankTaxCode_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> partyRegistryProxyClient.getInstitutionsByUserFiscalCode("  "));
        assertEquals("An user's fiscal code is required", ex.getMessage());
    }

    @Test
    void getAooById_delegatesToRestClient() {
        AooResponse expected = new AooResponse();
        expected.setCodAoo("AOO1");
        when(restClient.getAooById("AOO1")).thenReturn(expected);

        AooResponse result = partyRegistryProxyClient.getAooById("AOO1");

        assertSame(expected, result);
    }

    @Test
    void getUoById_delegatesToRestClient() {
        UoResponse expected = new UoResponse();
        expected.setUniUoCode("UO1");
        when(restClient.getUoById("UO1")).thenReturn(expected);

        UoResponse result = partyRegistryProxyClient.getUoById("UO1");

        assertSame(expected, result);
    }

    @Test
    void getExtById_delegatesToRestClient() {
        GeographicTaxonomiesResponse expected = new GeographicTaxonomiesResponse();
        expected.setGeotaxId("GEO1");
        when(restClient.getExtByCode("GEO1")).thenReturn(expected);

        GeographicTaxonomiesResponse result = partyRegistryProxyClient.getExtById("GEO1");

        assertSame(expected, result);
    }
}
