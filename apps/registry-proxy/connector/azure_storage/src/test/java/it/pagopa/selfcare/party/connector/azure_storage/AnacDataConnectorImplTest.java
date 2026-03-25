package it.pagopa.selfcare.party.connector.azure_storage;

import it.pagopa.selfcare.party.connector.azure_storage.client.AzureBlobClient;
import it.pagopa.selfcare.party.registry_proxy.connector.api.AnacDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.exception.ResourceNotFoundException;
import it.pagopa.selfcare.party.registry_proxy.connector.model.ResourceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnacDataConnectorImplTest {

    @Mock
    private AzureBlobClient azureBlobClientMock;

    @Test
    void getStations() {
        final String filename = "test.csv";
        AnacDataConnector anacDataConnector = new AnacDataConnectorImpl(filename, azureBlobClientMock);
        ResourceResponse response = new ResourceResponse();
        String bytes = """
                codice_IPA,cf_gestore,cenominazione,comicilio_digitale,anac_incaricato,anac_abilitato
                aaaaaaa,tax_code,denominazione,test@pec.it,false,false
                """;
        response.setData(bytes.getBytes(StandardCharsets.UTF_8));
        when(azureBlobClientMock.getFile(anyString())).thenReturn(response);
        final Optional<ByteArrayInputStream> inputStream = anacDataConnector.getANACData();
        assertNotNull(inputStream);
        verify(azureBlobClientMock, times(1)).getFile(filename);
        verifyNoMoreInteractions(azureBlobClientMock);
    }

    @Test
    void getStationsFileNotFound(){
        final String filename = "test.csv";
        AnacDataConnector anacDataConnector = new AnacDataConnectorImpl(filename, azureBlobClientMock);
        when(azureBlobClientMock.getFile(anyString())).thenThrow(ResourceNotFoundException.class);
        final  Optional<ByteArrayInputStream> inputStream = anacDataConnector.getANACData();
        assertTrue(inputStream.isEmpty());
        verify(azureBlobClientMock, times(1)).getFile(filename);
        verifyNoMoreInteractions(azureBlobClientMock);
    }


    @Test
    void getStationsWithFileName() {
        final String filename = "test.csv";
        AnacDataConnector anacDataConnector = new AnacDataConnectorImpl(filename, azureBlobClientMock);
        ResourceResponse response = new ResourceResponse();
        String bytes = """
                codice_IPA,cf_gestore,cenominazione,comicilio_digitale,anac_incaricato,anac_abilitato
                aaaaaaa,tax_code,denominazione,test@pec.it,false,false
                """;
        response.setData(bytes.getBytes(StandardCharsets.UTF_8));
        when(azureBlobClientMock.getFile(anyString())).thenReturn(response);
        final  Optional<ByteArrayInputStream> inputStream = anacDataConnector.getANACData();
        assertTrue(inputStream.isPresent());
        verify(azureBlobClientMock, times(1)).getFile(filename);
        verifyNoMoreInteractions(azureBlobClientMock);
    }

    @Test
    void getStationsFileNotFoundWithFileName() throws IOException {
        final String filename = "test.csv";
        AnacDataConnector anacDataConnector = new AnacDataConnectorImpl(filename, azureBlobClientMock);
        when(azureBlobClientMock.getFile(anyString())).thenThrow(ResourceNotFoundException.class);
        final  Optional<ByteArrayInputStream> inputStream = anacDataConnector.getANACData();
        assertTrue(inputStream.isEmpty());
        verify(azureBlobClientMock, times(1)).getFile(filename);
        verifyNoMoreInteractions(azureBlobClientMock);
    }
}
