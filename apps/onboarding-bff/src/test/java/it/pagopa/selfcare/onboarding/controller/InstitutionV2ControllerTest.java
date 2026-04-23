package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.controller.response.VerifyAggregatesResponse;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import it.pagopa.selfcare.onboarding.service.UserService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapi.quarkus.onboarding_json.model.VerifyAggregateResponse;

@ExtendWith(MockitoExtension.class)
class InstitutionV2ControllerTest {

    @Mock
    InstitutionService institutionService;
    @Mock
    UserService userService;
    @Mock
    OnboardingMapper onboardingMapper;
    @Mock
    InstitutionMapper institutionMapper;

    @InjectMocks
    InstitutionV2Controller institutionV2Controller;

    @Test
    void verifyAggregatesCsv_supportsLegacyQueryProductIdWhenFormProductIdMissing() throws Exception {
        Path tempFile = Files.createTempFile("aggregates-", ".csv");
        Files.writeString(tempFile, "taxCode;description\n123;demo");
        try {
            FileUpload fileUpload = Mockito.mock(FileUpload.class);
            when(fileUpload.fileName()).thenReturn("aggregates.csv");
            when(fileUpload.contentType()).thenReturn("text/csv");
            when(fileUpload.uploadedFile()).thenReturn(tempFile);

            VerifyAggregateResponse serviceResponse = new VerifyAggregateResponse();
            VerifyAggregatesResponse mappedResponse = new VerifyAggregatesResponse();
            when(institutionService.validateAggregatesCsv(any(UploadedFile.class), eq("prod-io"))).thenReturn(serviceResponse);
            when(onboardingMapper.toVerifyAggregatesResponse(serviceResponse)).thenReturn(mappedResponse);

            VerifyAggregatesResponse result = institutionV2Controller.verifyAggregatesCsv(
                    fileUpload,
                    null,
                    null,
                    "PA",
                    "prod-io"
            );

            assertSame(mappedResponse, result);
            verify(institutionService).validateAggregatesCsv(any(UploadedFile.class), eq("prod-io"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void verifyAggregatesCsv_throwsWhenProductIdMissing() throws Exception {
        Path tempFile = Files.createTempFile("aggregates-", ".csv");
        Files.writeString(tempFile, "taxCode;description\n123;demo");
        try {
            FileUpload fileUpload = Mockito.mock(FileUpload.class);

            assertThrows(InvalidRequestException.class, () ->
                    institutionV2Controller.verifyAggregatesCsv(fileUpload, "PA", null, null, null));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
