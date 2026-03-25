package it.pagopa.selfcare.party.connector.ftp;

import it.pagopa.selfcare.party.connector.azure_storage.client.AzureBlobClient;
import it.pagopa.selfcare.party.registry_proxy.connector.api.AnacDataConnector;
import it.pagopa.selfcare.party.registry_proxy.connector.api.FTPConnector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
@ConditionalOnProperty(
        value = "file.connector.type",
        havingValue = "sftp",
        matchIfMissing = true)
@PropertySource("classpath:config/ftp-config.properties")
public class AnacDataFromFTPConnectorImpl implements AnacDataConnector {

    private static final String ANAC_DATA_FILE_PREFIX = "collaudo-";
    private static final String ANAC_DATA_FILE_SUFFIX = ".csv";
    private final String directory;
    private final FTPConnector ftpConnector;
    private final AzureBlobClient azureBlobClient;

    private final String anacFileName;
    public AnacDataFromFTPConnectorImpl(@Value("${anac.ftp.directory}") String anacDirectory,
                                        @Value("${blobStorage.anac.filename}") String anacFileName,
                                        FTPConnector ftpConnector, AzureBlobClient azureBlobClient) {
        this.ftpConnector = ftpConnector;
        this.directory = anacDirectory;
        this.azureBlobClient = azureBlobClient;
        this.anacFileName = anacFileName;
    }


    /**
     * The getANACData function is used to retrieve the ANAC data from the FTP server.
     * The function first creates a file name based on the current date and time, then uses this file name to get an InputStream of that file from the FTP server.
     * If there is no such file, it returns an empty Optional InputStream. Otherwise, it uploads that InputStream to Azure Blob Storage to update the last ANAC file and returns it as well.
     */
    @Override
    public Optional<ByteArrayInputStream> getANACData() {
        String fileName = createFileName();
        log.trace("getANACDataFromFTP on filename: {} start", fileName);
        Optional<InputStream> optionalFile = ftpConnector.getFile(directory + fileName);
        return optionalFile.flatMap(inputStream -> {
            Optional<ByteArrayInputStream> opt = updateFileOnAzureStorageAndRetrieveInputStream(inputStream);
            log.debug("getANACDataFromFTP on filename from ftp: {} end", fileName);
            return opt;
        });

    }

    private Optional<ByteArrayInputStream> updateFileOnAzureStorageAndRetrieveInputStream(InputStream inputStream){
        try {
            log.info("START upload file on azure after download from FTP");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, out);
            byte[] bytes = out.toByteArray();
            azureBlobClient.uploadFile(new ByteArrayInputStream(bytes), anacFileName);
            log.info("END upload file on azure after download from FTP");
            return Optional.of(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            log.error("Error during retrieving ANAC csv from FTP. Error: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    private String createFileName() {
        StringBuilder fileNameBuilder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return fileNameBuilder.append(ANAC_DATA_FILE_PREFIX)
                .append(LocalDateTime.now().format(formatter))
                .append(ANAC_DATA_FILE_SUFFIX)
                .toString();
    }
}
