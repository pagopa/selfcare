package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.dashboard.config.CoreTestConfig;
import it.pagopa.selfcare.dashboard.connector.api.FileStorageConnector;
import it.pagopa.selfcare.dashboard.exception.FileUploadException;
import it.pagopa.selfcare.dashboard.exception.FileValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.MimeTypeUtils;

import java.io.InputStream;

@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {FileStorageServiceImpl.class, CoreTestConfig.class})
@TestPropertySource(properties = {
        "INSTITUTION_LOGO_ALLOWED_MIME_TYPES=image/png,image/jpeg",
        "INSTITUTION_LOGO_ALLOWED_EXTENSIONS=png,jpeg"
})
class FileStorageServiceImplTest {

    @MockBean
    private FileStorageConnector storageConnectorMock;

    @Autowired
    private FileStorageServiceImpl storageService;


    @Test
    void storeInstitutionLogo_nullFileName() {
        String institutionId = "institutionId";
        InputStream logo = InputStream.nullInputStream();
        // when
        Executable executable = () -> storageService.storeInstitutionLogo(institutionId, logo, null, null);
        // then
        Assertions.assertThrows(FileValidationException.class, executable);
        Mockito.verifyNoInteractions(storageConnectorMock);
    }


    @Test
    void storeInstitutionLogo_invalidMimeType() {
        // given
        String institutionId = "institutionId";
        InputStream logo = InputStream.nullInputStream();
        String contentType = MimeTypeUtils.IMAGE_GIF_VALUE;
        String fileName = "filename";
        // when
        Executable executable = () -> storageService.storeInstitutionLogo(institutionId, logo, contentType, fileName);
        // then
        Assertions.assertThrows(FileValidationException.class, executable);
        Mockito.verifyNoInteractions(storageConnectorMock);
    }


    @Test
    void storeInstitutionLogo_invalidExtension() {
        // given
        String institutionId = "institutionId";
        InputStream logo = InputStream.nullInputStream();
        String contentType = MimeTypeUtils.IMAGE_PNG_VALUE;
        String fileName = "filename.gif";
        // when
        Executable executable = () -> storageService.storeInstitutionLogo(institutionId, logo, contentType, fileName);
        // then
        Assertions.assertThrows(FileValidationException.class, executable);
        Mockito.verifyNoInteractions(storageConnectorMock);
    }


    @Test
    void storeInstitutionLogo_uploadException() throws FileUploadException {
        // given
        String institutionId = "institutionId";
        InputStream logo = InputStream.nullInputStream();
        String contentType = MimeTypeUtils.IMAGE_PNG_VALUE;
        String fileName = "filename.png";
        Mockito.doThrow(FileUploadException.class)
                .when(storageConnectorMock).uploadInstitutionLogo(Mockito.any(), Mockito.any(), Mockito.any());
        // when
        Executable executable = () -> storageService.storeInstitutionLogo(institutionId, logo, contentType, fileName);
        // then
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, executable);
        Assertions.assertNotNull(exception.getCause());
        Assertions.assertTrue(FileUploadException.class.isAssignableFrom(exception.getCause().getClass()));
        Mockito.verify(storageConnectorMock, Mockito.times(1))
                .uploadInstitutionLogo(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verifyNoMoreInteractions(storageConnectorMock);
    }


    @Test
    void storeInstitutionLogo() throws FileUploadException {
        // given
        String institutionId = "institutionId";
        InputStream logo = InputStream.nullInputStream();
        String contentType = MimeTypeUtils.IMAGE_PNG_VALUE;
        String fileName = "filename.png";
        // when
        storageService.storeInstitutionLogo(institutionId, logo, contentType, fileName);
        // then
        Mockito.verify(storageConnectorMock, Mockito.times(1))
                .uploadInstitutionLogo(Mockito.any(), Mockito.eq("institutions/" + institutionId + "/logo.png"), Mockito.eq(contentType));
        Mockito.verifyNoMoreInteractions(storageConnectorMock);
    }

}