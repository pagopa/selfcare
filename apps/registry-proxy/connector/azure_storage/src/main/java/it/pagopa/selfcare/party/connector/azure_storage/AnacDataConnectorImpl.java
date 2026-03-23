package it.pagopa.selfcare.party.connector.azure_storage;

import it.pagopa.selfcare.party.registry_proxy.connector.api.AnacDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.FileStorageConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.model.ResourceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Optional;

@Service
@Slf4j
@ConditionalOnProperty(
        value = "file.connector.type",
        havingValue = "azure")
@PropertySource("classpath:config/ftp-config.properties")
public class AnacDataConnectorImpl implements AnacDataConnector {

    private final FileStorageConnector fileStorageConnector;
    private final String fileName;

    public AnacDataConnectorImpl(@Value("${blobStorage.anac.filename}") String azureAnacFileName,
                                 FileStorageConnector fileStorageConnector) {
        this.fileStorageConnector = fileStorageConnector;
        this.fileName = azureAnacFileName;
    }

    @Override
    public Optional<ByteArrayInputStream> getANACData() {
        ResourceResponse resourceResponse;
        try {
            resourceResponse = fileStorageConnector.getFile(fileName);
            return Optional.of(new ByteArrayInputStream(resourceResponse.getData()));
        } catch (Exception e) {
            log.error("Impossible to retrieve file ANAC from Storage. Error: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }
}
