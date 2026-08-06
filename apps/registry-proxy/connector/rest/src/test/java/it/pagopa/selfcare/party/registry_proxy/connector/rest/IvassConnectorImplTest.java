package it.pagopa.selfcare.party.registry_proxy.connector.rest;

import it.pagopa.selfcare.party.registry_proxy.connector.api.FileStorageConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.InsuranceCompany;
import it.pagopa.selfcare.party.registry_proxy.connector.model.ResourceResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.client.IvassRestClient;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.exception.IvassFileParseException;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.model.IvassDataTemplate;
import it.pagopa.selfcare.party.registry_proxy.connector.rest.utils.IvassUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IvassConnectorImplTest {
    private static final String IVASS_AZURE_FILENAME = "ivass-data.csv";

    private IvassRestClient ivassRestClient;
    private IvassUtils ivassUtils;
    private FileStorageConnector fileStorageConnector;
    private IvassConnectorImpl ivassConnector;

    @BeforeEach
    void setUp() {
        this.ivassRestClient = mock(IvassRestClient.class);
        this.ivassUtils = mock(IvassUtils.class);
        this.fileStorageConnector = mock(FileStorageConnector.class);
        List<String> registryTypes = Arrays.asList("ElencoI", "ElencoII", "SezioneI", "SezioneII");
        List<String> workTypes = Arrays.asList("VITA", "PICCOLO CUMULO", "MISTA");
        ivassConnector = new IvassConnectorImpl(ivassRestClient, registryTypes, workTypes, ivassUtils,
                fileStorageConnector, IVASS_AZURE_FILENAME);
    }

    @Test
    void getInsurances_shouldReturnFilteredCompanies() {
        byte[] zip = new byte[]{0, 1, 2, 3, 4};
        byte[] csv = new byte[]{5, 6, 7, 8, 9};

        IvassDataTemplate company1 = new IvassDataTemplate();
        company1.setDigitalAddress("digitalAddress1");
        company1.setWorkType("VITA");
        company1.setRegisterType("ElencoI - test");
        company1.setTaxCode("taxCode1");

        IvassDataTemplate company2 = new IvassDataTemplate();
        company2.setDigitalAddress("digitalAddress2");
        company2.setWorkType("VITA");
        company2.setRegisterType("ElencoII - test");
        company2.setTaxCode("taxCode2");

        List<InsuranceCompany> companies = Arrays.asList(company1, company2);

        when(ivassRestClient.getInsurancesZip()).thenReturn(zip);
        when(ivassUtils.extractFirstEntryByteArrayFromZip(zip)).thenReturn(csv);
        when(ivassUtils.manageUTF8BOM(csv)).thenReturn(csv);
        when(ivassUtils.readCsv(csv)).thenReturn(companies);

        List<InsuranceCompany> result = ivassConnector.getInsurances();

        assertEquals(companies.size(), result.size());
        verify(ivassRestClient, times(1)).getInsurancesZip();
        verify(ivassUtils, times(1)).extractFirstEntryByteArrayFromZip(zip);
        // A successful parse with non-empty result should trigger a backup to Azure
        verify(fileStorageConnector, times(1)).uploadFile(any(), eq(IVASS_AZURE_FILENAME));
    }

    @Test
    void getInsurances_shouldReturnEmptyList_whenNoCompaniesMatchFilter() {
        byte[] zip = new byte[]{0, 1, 2, 3, 4};
        byte[] csv = new byte[]{5, 6, 7, 8, 9};
        // Companies with missing/invalid fields that will be filtered out
        List<InsuranceCompany> companies = Arrays.asList(new IvassDataTemplate(), new IvassDataTemplate());

        when(ivassRestClient.getInsurancesZip()).thenReturn(zip);
        when(ivassUtils.extractFirstEntryByteArrayFromZip(zip)).thenReturn(csv);
        when(ivassUtils.manageUTF8BOM(csv)).thenReturn(csv);
        when(ivassUtils.readCsv(csv)).thenReturn(companies);

        ivassConnector = spy(ivassConnector);

        List<InsuranceCompany> result = ivassConnector.getInsurances();

        assertEquals(0, result.size());
        verify(ivassRestClient, times(1)).getInsurancesZip();
        verify(ivassUtils, times(1)).extractFirstEntryByteArrayFromZip(zip);
        // The raw parse result is non-empty, so a backup to Azure is still performed:
        // the file is structurally valid even if no companies pass the filter criteria.
        verify(fileStorageConnector, times(1)).uploadFile(any(), eq(IVASS_AZURE_FILENAME));
    }

    @Test
    void getInsurances_shouldFallbackToAzure_whenRestCsvIsNotCompliant() {
        byte[] zip = new byte[]{0, 1, 2, 3, 4};
        byte[] csv = new byte[]{5, 6, 7, 8, 9};

        IvassDataTemplate company = new IvassDataTemplate();
        company.setDigitalAddress("pec@example.com");
        company.setWorkType("VITA");
        company.setRegisterType("ElencoI - fallback");
        company.setTaxCode("12345678901");

        ResourceResponse azureResponse = new ResourceResponse();
        azureResponse.setData(csv);

        when(ivassRestClient.getInsurancesZip()).thenReturn(zip);
        when(ivassUtils.extractFirstEntryByteArrayFromZip(zip)).thenReturn(csv);
        when(ivassUtils.manageUTF8BOM(csv)).thenReturn(csv);
        // First call (from REST): parse fails; second call (from Azure fallback): succeeds
        when(ivassUtils.readCsv(csv))
                .thenThrow(new IvassFileParseException("malformed CSV", new RuntimeException()))
                .thenReturn(Collections.singletonList(company));
        when(fileStorageConnector.getFile(IVASS_AZURE_FILENAME)).thenReturn(azureResponse);

        List<InsuranceCompany> result = ivassConnector.getInsurances();

        assertEquals(1, result.size());
        verify(fileStorageConnector, times(1)).getFile(IVASS_AZURE_FILENAME);
        // No backup upload should happen when we are using the fallback
        verify(fileStorageConnector, never()).uploadFile(any(), any());
    }

    @Test
    void getInsurances_shouldReturnEmptyList_whenRestCsvIsNotCompliantAndAzureFallbackFails() {
        byte[] zip = new byte[]{0, 1, 2, 3, 4};
        byte[] csv = new byte[]{5, 6, 7, 8, 9};

        when(ivassRestClient.getInsurancesZip()).thenReturn(zip);
        when(ivassUtils.extractFirstEntryByteArrayFromZip(zip)).thenReturn(csv);
        when(ivassUtils.manageUTF8BOM(csv)).thenReturn(csv);
        when(ivassUtils.readCsv(csv)).thenThrow(new IvassFileParseException("malformed CSV", new RuntimeException()));
        when(fileStorageConnector.getFile(IVASS_AZURE_FILENAME)).thenThrow(new RuntimeException("Azure unavailable"));

        List<InsuranceCompany> result = ivassConnector.getInsurances();

        assertTrue(result.isEmpty());
        verify(fileStorageConnector, times(1)).getFile(IVASS_AZURE_FILENAME);
    }

    @Test
    void getInsurances_shouldReturnEmptyList_whenRestCsvIsNotCompliantAndNoAzureConfigured() {
        IvassConnectorImpl connectorWithoutAzure = new IvassConnectorImpl(
                ivassRestClient,
                Arrays.asList("ElencoI", "ElencoII", "SezioneI", "SezioneII"),
                Arrays.asList("VITA", "PICCOLO CUMULO", "MISTA"),
                ivassUtils, null, IVASS_AZURE_FILENAME);

        byte[] zip = new byte[]{0, 1, 2, 3, 4};
        byte[] csv = new byte[]{5, 6, 7, 8, 9};

        when(ivassRestClient.getInsurancesZip()).thenReturn(zip);
        when(ivassUtils.extractFirstEntryByteArrayFromZip(zip)).thenReturn(csv);
        when(ivassUtils.manageUTF8BOM(csv)).thenReturn(csv);
        when(ivassUtils.readCsv(csv)).thenThrow(new IvassFileParseException("malformed CSV", new RuntimeException()));

        List<InsuranceCompany> result = connectorWithoutAzure.getInsurances();

        assertTrue(result.isEmpty());
    }
}
