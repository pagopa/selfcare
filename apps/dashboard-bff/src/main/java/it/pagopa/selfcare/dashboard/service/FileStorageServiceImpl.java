package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.connector.api.FileStorageConnector;
import it.pagopa.selfcare.dashboard.exception.FileUploadException;
import it.pagopa.selfcare.dashboard.exception.FileValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.Set;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    public static final String LOGO_PATH_TEMPLATE = "institutions/%s/logo.%s";

    private final FileStorageConnector fileStorageConnector;
    private final Set<String> allowedInstitutionLogoMimeTypes;
    private final Set<String> allowedInstitutionLogoExtensions;


    @Autowired
    public FileStorageServiceImpl(FileStorageConnector fileStorageConnector,
                                  @Value("${dashboard.institution.logo.allowed-mime-types}") String[] allowedInstitutionLogoMimeTypes,
                                  @Value("${dashboard.institution.logo.allowed-extensions}") String[] allowedInstitutionLogoExtensions) {
        this.fileStorageConnector = fileStorageConnector;
        this.allowedInstitutionLogoMimeTypes = Set.of(allowedInstitutionLogoMimeTypes);
        this.allowedInstitutionLogoExtensions = Set.of(allowedInstitutionLogoExtensions);
    }


    @Override
    public void storeInstitutionLogo(String institutionId, InputStream logo, String contentType, String fileName) {
        try {
            validate(contentType, fileName);

        } catch (Exception e) {
            throw new FileValidationException(e.getMessage(), e);
        }

        String fileExtension = StringUtils.getFilenameExtension(fileName);
        try {
            fileStorageConnector.uploadInstitutionLogo(logo, String.format(LOGO_PATH_TEMPLATE, institutionId, fileExtension), contentType);

        } catch (FileUploadException e) {
            throw new RuntimeException(e);
        }
    }


    private void validate(String contentType, String fileName) {
        Assert.notNull(fileName, "file name cannot be null");

        if (!allowedInstitutionLogoMimeTypes.contains(contentType)) {
            throw new InvalidMimeTypeException(contentType, String.format("allowed only %s", allowedInstitutionLogoMimeTypes));
        }

        String fileExtension = StringUtils.getFilenameExtension(fileName);
        if (!allowedInstitutionLogoExtensions.contains(fileExtension)) {
            throw new IllegalArgumentException(String.format("Invalid file extension \"%s\": allowed only %s", fileExtension, allowedInstitutionLogoExtensions));
        }
    }

}
