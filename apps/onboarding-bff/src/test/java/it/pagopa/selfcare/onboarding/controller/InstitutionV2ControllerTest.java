package it.pagopa.selfcare.onboarding.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.onboarding.client.model.InstitutionProxyInfo;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.smallrye.mutiny.subscription.UniEmitter;
import it.pagopa.selfcare.onboarding.client.model.OnboardingData;
import it.pagopa.selfcare.onboarding.model.dto.request.OnboardingProductDto;
import it.pagopa.selfcare.onboarding.client.model.IpaInstitutionsSearchResult;
import it.pagopa.selfcare.onboarding.client.model.UploadedFile;
import it.pagopa.selfcare.onboarding.client.model.VerifyAggregateResult;
import it.pagopa.selfcare.onboarding.model.dto.response.IpaInstitutionResource;
import it.pagopa.selfcare.onboarding.model.dto.response.IpaInstitutionsSearchResource;
import it.pagopa.selfcare.onboarding.model.dto.response.VerifyAggregatesResponse;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.mapper.OnboardingMapper;
import it.pagopa.selfcare.onboarding.mapper.RegistryProxyMapper;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitutionV2ControllerTest {

    @Mock
    InstitutionService institutionService;
    @Mock
    OnboardingMapper onboardingMapper;
    @Mock
    InstitutionMapper institutionMapper;
    @Mock
    RegistryProxyMapper registryProxyMapper;

    private InstitutionV2Controller controller() {
        return new InstitutionV2Controller(institutionService, onboardingMapper, institutionMapper, registryProxyMapper);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Timeout(value = 2, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void onboardingWaitsForValidationBeforeMappingAndDispatch(boolean aggregator) {
        // given
        AtomicReference<UniEmitter<? super Void>> pending = new AtomicReference<>();
        OnboardingProductDto request = new OnboardingProductDto();
        request.setTaxCode("00000000000");
        request.setProductId("prod-test");
        request.setIsAggregator(aggregator);
        OnboardingData data = new OnboardingData();
        when(institutionService.validateOnboardingByProductOrInstitutionTaxCode("00000000000", "prod-test"))
                .thenReturn(Uni.createFrom().<Void>emitter(pending::set));
        when(onboardingMapper.toEntity(request)).thenReturn(data);
        if (aggregator) {
            when(institutionService.onboardingPaAggregator(data)).thenReturn(Uni.createFrom().voidItem());
        } else {
            when(institutionService.onboardingProductV2(data)).thenReturn(Uni.createFrom().voidItem());
        }

        // when
        UniAssertSubscriber<Response> result = controller().onboarding(request)
                .subscribe().withSubscriber(UniAssertSubscriber.create());

        // then
        result.assertNotTerminated();
        verifyNoInteractions(onboardingMapper);
        verify(institutionService, never()).onboardingPaAggregator(any());
        verify(institutionService, never()).onboardingProductV2(any());
        pending.get().complete(null);
        result.assertCompleted();
        assertEquals(201, result.getItem().getStatus());
        if (aggregator) {
            verify(institutionService).onboardingPaAggregator(data);
            verify(institutionService, never()).onboardingProductV2(any());
        } else {
            verify(institutionService).onboardingProductV2(data);
            verify(institutionService, never()).onboardingPaAggregator(any());
        }
    }

    @Test
    void searchIpaInstitutions_appliesTheSpringDefaults() {
        IpaInstitutionsSearchResult result = new IpaInstitutionsSearchResult();
        IpaInstitutionsSearchResource resource = new IpaInstitutionsSearchResource();
        when(institutionService.searchIpaInstitutions("*", null, 0, 50)).thenReturn(result);
        when(registryProxyMapper.toResource(result)).thenReturn(resource);

        assertSame(resource, controller().searchIpaInstitutions(null, null, null, null));
        verify(institutionService).searchIpaInstitutions("*", null, 0, 50);
    }

    @Test
    void searchIpaInstitutions_emptySearchFallsBackToTheWildcard() {
        controller().searchIpaInstitutions("", "C17,C16", "2", "10");

        verify(institutionService).searchIpaInstitutions("*", "C17,C16", 2, 10);
    }

    @Test
    void searchIpaInstitutions_forwardsTheGivenValues() {
        controller().searchIpaInstitutions("esempio", "C17,C16", "1", "20");

        verify(institutionService).searchIpaInstitutions("esempio", "C17,C16", 1, 20);
    }

    @Test
    void searchIpaInstitutions_nonNumericPagingIsABadRequestWithoutDownstreamCalls() {
        InstitutionV2Controller controller = controller();

        assertThrows(InvalidRequestException.class, () -> controller.searchIpaInstitutions("a", null, "x", null));
        assertThrows(InvalidRequestException.class, () -> controller.searchIpaInstitutions("a", null, null, "1.5"));
        verifyNoInteractions(institutionService);
    }

    @Test
    void findIpaInstitutionByTaxCode_forwardsTaxCodeAndCategory() {
        InstitutionProxyInfo info = new InstitutionProxyInfo();
        IpaInstitutionResource resource = new IpaInstitutionResource();
        when(institutionService.findIpaInstitutionByTaxCode("12345678901", "C17")).thenReturn(info);
        when(registryProxyMapper.toResource(info)).thenReturn(resource);

        assertSame(resource, controller().findIpaInstitutionByTaxCode("12345678901", "C17"));
    }

    @Test
    void findIpaInstitutionByTaxCode_categoryIsOptional() {
        controller().findIpaInstitutionByTaxCode("12345678901", null);

        verify(institutionService).findIpaInstitutionByTaxCode("12345678901", null);
    }

    @Test
    void getInstitution_requiresProductId() {
        InstitutionV2Controller controller = controller();

        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> controller.getInstitution(null, "tax", null, null, null));

        assertEquals("Required request parameter 'productId' for method parameter type String is not present",
                e.getMessage());
        verifyNoInteractions(institutionService);
    }

    @Test
    void getActiveOnboarding_requiresTaxCodeAndProductId() {
        InstitutionV2Controller controller = controller();

        assertThrows(InvalidRequestException.class, () -> controller.getActiveOnboarding(null, "prod", null));
        assertThrows(InvalidRequestException.class, () -> controller.getActiveOnboarding("tax", null, null));
        assertThrows(InvalidRequestException.class, () -> controller.getActiveOnboarding(" ", "prod", null));
        verifyNoInteractions(institutionService);
    }

    @Test
    void checkRecipientCode_requiresBothParameters() {
        InstitutionV2Controller controller = controller();

        assertThrows(InvalidRequestException.class, () -> controller.checkRecipientCode(null, "RC"));
        assertThrows(InvalidRequestException.class, () -> controller.checkRecipientCode("origin", null));
        verifyNoInteractions(institutionService);
    }

    @Test
    void getOnboardingsInfo_requiresTaxCodeAndStatus() {
        InstitutionV2Controller controller = controller();

        assertThrows(InvalidRequestException.class, () -> controller.getOnboardingsInfo(null, "PENDING"));
        assertThrows(InvalidRequestException.class, () -> controller.getOnboardingsInfo("tax", null));
        verifyNoInteractions(institutionService);
    }

    @Test
    void verifyAggregatesCsv_supportsLegacyQueryProductIdWhenFormProductIdMissing() throws Exception {
        Path tempFile = Files.createTempFile("aggregates-", ".csv");
        Files.writeString(tempFile, "taxCode;description\n123;demo");
        try {
            FileUpload fileUpload = csv(tempFile);
            VerifyAggregateResult serviceResponse = new VerifyAggregateResult();
            VerifyAggregatesResponse mappedResponse = new VerifyAggregatesResponse();
            when(institutionService.validateAggregatesCsv(any(UploadedFile.class), eq("prod-io"))).thenReturn(serviceResponse);
            when(onboardingMapper.toVerifyAggregatesResponse(serviceResponse)).thenReturn(mappedResponse);

            VerifyAggregatesResponse result = controller().verifyAggregatesCsv(fileUpload, null, null, "PA", "prod-io");

            assertSame(mappedResponse, result);
            verify(institutionService).validateAggregatesCsv(any(UploadedFile.class), eq("prod-io"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void verifyAggregatesCsv_formProductIdWinsOverTheLegacyQuery() throws Exception {
        Path tempFile = Files.createTempFile("aggregates-", ".csv");
        Files.writeString(tempFile, "taxCode;description\n123;demo");
        try {
            FileUpload fileUpload = csv(tempFile);
            when(institutionService.validateAggregatesCsv(any(UploadedFile.class), eq("prod-pn"))).thenReturn(new VerifyAggregateResult());

            controller().verifyAggregatesCsv(fileUpload, "PA", "prod-pn", null, "prod-io");

            verify(institutionService).validateAggregatesCsv(any(UploadedFile.class), eq("prod-pn"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void verifyAggregatesCsv_throwsWhenProductIdMissing() throws Exception {
        Path tempFile = Files.createTempFile("aggregates-", ".csv");
        Files.writeString(tempFile, "taxCode;description\n123;demo");
        try {
            FileUpload fileUpload = csv(tempFile);
            InstitutionV2Controller controller = controller();

            assertThrows(InvalidRequestException.class,
                    () -> controller.verifyAggregatesCsv(fileUpload, "PA", null, null, null));
            verifyNoInteractions(institutionService);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void verifyAggregatesCsv_throwsWhenTheFilePartIsMissing() {
        InstitutionV2Controller controller = controller();

        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> controller.verifyAggregatesCsv(null, "PA", "prod-io", null, null));

        assertEquals("Required part 'aggregates' is not present.", e.getMessage());
        verifyNoInteractions(institutionService);
    }

    @Test
    void verifyAggregatesCsv_rejectsUnsupportedFileFormats() throws Exception {
        Path tempFile = Files.createTempFile("aggregates-", ".txt");
        Files.writeString(tempFile, "demo");
        try {
            FileUpload fileUpload = mock(FileUpload.class);
            when(fileUpload.fileName()).thenReturn("aggregates.txt");
            when(fileUpload.contentType()).thenReturn("text/plain");
            when(fileUpload.uploadedFile()).thenReturn(tempFile);
            InstitutionV2Controller controller = controller();

            assertThrows(InvalidRequestException.class,
                    () -> controller.verifyAggregatesCsv(fileUpload, "PA", "prod-io", null, null));
            verifyNoInteractions(institutionService);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void triggerOnboardingRequest_answersNoContent() {
        assertEquals(204, controller().triggerOnboardingRequest("onb-1").getStatus());

        verify(institutionService).triggerOnboardingRequest("onb-1");
    }

    @Test
    void getInstitution_mapsTheFilteredInstitutions() {
        when(institutionService.getByFilters("prod", "tax", null, null, null)).thenReturn(List.of());

        assertEquals(List.of(), controller().getInstitution("prod", "tax", null, null, null));
    }

    private static FileUpload csv(Path tempFile) {
        FileUpload fileUpload = mock(FileUpload.class);
        when(fileUpload.fileName()).thenReturn("aggregates.csv");
        when(fileUpload.contentType()).thenReturn("text/csv");
        when(fileUpload.uploadedFile()).thenReturn(tempFile);
        return fileUpload;
    }
}
