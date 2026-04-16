package it.pagopa.selfcare.party.registry_proxy.core;

import it.pagopa.selfcare.party.registry_proxy.connector.api.AnacDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.FileStorageConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.function.Executable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ANACServiceImplTest {

    private ANACServiceImpl anacService;

    @Mock
    private FileStorageConnector fileStorageConnector;

    private String fileStorageFileName = "fileStorageFileName";

    @Mock
    private AnacDataConnector anacDataConnector;

    @Mock
    private IndexWriterService<Station> stationIndexWriterService;

    private static final String testCaseCsv = "codiceFiscaleGestore,denominazioneGestore,PEC,codiceIPA,ANAC_incaricato,ANAC_in_convalida,ANAC_abilitato\n" +
            "00000000000,srl s.r.l.,srl@pec.it,,true,false,false";

    @Test
    void loadStations() {
        anacService = new ANACServiceImpl(anacDataConnector, fileStorageConnector, fileStorageFileName, stationIndexWriterService);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testCaseCsv.getBytes(StandardCharsets.UTF_8));
        when(anacDataConnector.getANACData()).thenReturn(Optional.of(inputStream));
        Executable executable = () -> anacService.loadStations();
        Assertions.assertDoesNotThrow(executable);
    }

    @Test
    void getStationsEmpty() {
        anacService = new ANACServiceImpl(anacDataConnector, fileStorageConnector, fileStorageFileName, stationIndexWriterService);
        when(anacDataConnector.getANACData()).thenReturn(Optional.empty());
        List<Station> stationList = anacService.loadStations();
        Assertions.assertEquals(0, stationList.size());
    }

    @Test
    void updateStationIndex() {
        anacService = new ANACServiceImpl(anacDataConnector, fileStorageConnector, fileStorageFileName, stationIndexWriterService);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testCaseCsv.getBytes(StandardCharsets.UTF_8));
        when(anacDataConnector.getANACData()).thenReturn(Optional.of(inputStream));
        Executable executable = () -> anacService.updateStationIndex();
        Assertions.assertDoesNotThrow(executable);
    }
}
