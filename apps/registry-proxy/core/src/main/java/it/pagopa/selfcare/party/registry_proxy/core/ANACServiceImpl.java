package it.pagopa.selfcare.party.registry_proxy.core;

import com.opencsv.bean.CsvToBeanBuilder;
import it.pagopa.selfcare.party.registry_proxy.connector.api.AnacDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.FileStorageConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.IndexWriterService;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AnacDataTemplate;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Entity;
import it.pagopa.selfcare.party.registry_proxy.connector.model.ResourceResponse;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Station;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ANACServiceImpl implements ANACService {

    private final AnacDataConnector anacDataConnector;
    private final FileStorageConnector fileStorageConnector;
    private final String fileStorageFileName;
    private final IndexWriterService<Station> stationIndexWriterService;

    public ANACServiceImpl(AnacDataConnector anacDataConnector,
                           FileStorageConnector fileStorageConnector,
                           @Value("${blobStorage.anac.filename}") String fileStorageFileName,
                           IndexWriterService<Station> stationIndexWriterService) {
        this.anacDataConnector = anacDataConnector;
        this.fileStorageConnector = fileStorageConnector;
        this.fileStorageFileName = fileStorageFileName;
        this.stationIndexWriterService = stationIndexWriterService;
    }

    @Scheduled(cron = "0 0 0/6 * * *")
    void updateStationIndex() {
        log.trace("start update ANAC Stations index");
        List<Station> stations = getStations();
        if (!stations.isEmpty()) {
            stationIndexWriterService.cleanIndex(Entity.STATION.toString());
            stationIndexWriterService.adds(stations);
        }
        log.trace("updated ANAC Stations index end");
    }

    /**
     * The loadStations function is used to load the stations on application startup. If file.connector.type properties is SFTP,
     * firstly it tries to retrieve anac file from server ftp,
     * if today's file is not present, the function retrieves the last uploaded file on azure storage.
     * Otherwise, if file.connector.type properties is AZURE the function retrieves the last uploaded file on azure storage.
     */
    @Override
    public List<Station> loadStations() {
        List<Station> stations = getStations();
        if (stations.isEmpty()) {
            return loadStationsFromAzureStorage();
        }
        return stations;
    }

    /**
     * The getStations function is used to retrieve the stations from Ftp or Azure storage
     * according to properties file.connector.type, that can assume the value SFTP or AZURE
     */
    public List<Station> getStations() {
        Optional<ByteArrayInputStream> optionalData = anacDataConnector.getANACData();
        return optionalData.map(data -> {
                    List<Station> stations = retrieveStationListFromCSV(data);
                    log.debug("getStations result = {}", stations);
                    log.trace("getStations end");
                    return stations;
                })
                .orElseGet(Collections::emptyList);
    }

    private List<Station> loadStationsFromAzureStorage() {
        ResourceResponse resourceResponse;
        try {
            resourceResponse = fileStorageConnector.getFile(fileStorageFileName);
            return retrieveStationListFromCSV(new ByteArrayInputStream(resourceResponse.getData()));
        } catch (Exception e) {
            log.error("Error during retrieving ANAC csv. Error: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private static List<Station> retrieveStationListFromCSV(ByteArrayInputStream byteArrayInputStream) {
        Reader reader = new InputStreamReader(byteArrayInputStream);
        CsvToBeanBuilder<Station> csvToBeanBuilder = new CsvToBeanBuilder<>(reader);
        csvToBeanBuilder.withType(AnacDataTemplate.class);
        csvToBeanBuilder.withIgnoreLeadingWhiteSpace(true);
        return csvToBeanBuilder.build()
                .parse()
                .stream()
                .filter(station -> !StringUtils.hasText(station.getOriginId()))
                .toList();
    }
}
