package it.pagopa.selfcare.onboarding.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.onboarding.HttpResponseMessageMock;
import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignRequest;
import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignResponse;
import it.pagopa.selfcare.onboarding.exception.GenericOnboardingException;
import it.pagopa.selfcare.onboarding.service.ContractUpdateCampaignService;
import it.pagopa.selfcare.onboarding.service.FunctionInvocationLogger;
import it.pagopa.selfcare.onboarding.service.CallerIdentity;
import it.pagopa.selfcare.onboarding.service.HttpAuthorization;
import it.pagopa.selfcare.onboarding.service.HttpJwtValidator;
import it.pagopa.selfcare.onboarding.service.TelemetryService;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

@QuarkusTest
class ContractUpdateCampaignFunctionsTest {

    private static final String VALID_BODY = "{\"institutionId\":\"institution-id\",\"productIds\":[\"prod-io\",\"prod-pagopa\"]}";

    @Inject
    ContractUpdateCampaignFunctions function;
    @InjectMock
    ContractUpdateCampaignService contractUpdateCampaignService;
    @InjectMock
    HttpJwtValidator httpJwtValidator;
    @InjectMock
    TelemetryService telemetryService;

    private final ExecutionContext context = mock(ExecutionContext.class);

    @BeforeEach
    void setup() {
        when(httpJwtValidator.authenticate(any(), any()))
            .thenReturn(
                HttpAuthorization.authorized(
                    new CallerIdentity("requester-id", "SPID", null, null, null)));
        doReturn(Logger.getGlobal()).when(context).getLogger();
    }

    @Test
    void sendContractUpdateMail_shouldReturnOutcomeAsJson() {
        when(contractUpdateCampaignService.send(any()))
                .thenReturn(new ContractUpdateCampaignResponse(ContractUpdateCampaignResponse.Outcome.SENT, "ente@pec.it"));

        HttpResponseMessage response = function.sendContractUpdateMail(request(VALID_BODY, "AR"), context);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        assertEquals("{\"outcome\":\"SENT\",\"digitalAddress\":\"ente@pec.it\"}", response.getBody());
        verify(contractUpdateCampaignService)
                .send(new ContractUpdateCampaignRequest("institution-id", List.of("prod-io", "prod-pagopa")));
        verify(telemetryService).trackFunction(eq("SendContractUpdateMail"), any(), eq(SeverityLevel.Information),
                eq(Map.of("institutionId", "institution-id", "productIds", "prod-io,prod-pagopa",
                        "userId", "requester-id", "outcome", "SENT")));
    }

    @Test
    void sendContractUpdateMail_shouldNotRequireUserIdHeader() {
        when(contractUpdateCampaignService.send(any()))
                .thenReturn(new ContractUpdateCampaignResponse(ContractUpdateCampaignResponse.Outcome.SENT, "ente@pec.it"));
        HttpRequestMessage<Optional<String>> req = request(VALID_BODY, "AR");
        doReturn(Map.of("X-Tenant-Id", "AR")).when(req).getHeaders();

        HttpResponseMessage response = function.sendContractUpdateMail(req, context);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode());
        verify(telemetryService).trackFunction(eq("SendContractUpdateMail"), any(), eq(SeverityLevel.Information),
                eq(Map.of("institutionId", "institution-id", "productIds", "prod-io,prod-pagopa",
                        "userId", "requester-id", "outcome", "SENT")));
    }

    @Test
    void sendContractUpdateMail_shouldRejectMalformedBody() {
        HttpResponseMessage response = function.sendContractUpdateMail(request("{invalid", "AR"), context);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode());
        verifyNoInteractions(contractUpdateCampaignService);
    }

    @Test
    void sendContractUpdateMail_shouldRejectMissingProductIds() {
        HttpResponseMessage response = function.sendContractUpdateMail(
                request("{\"institutionId\":\"institution-id\",\"productIds\":[]}", "AR"), context);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode());
        verifyNoInteractions(contractUpdateCampaignService);
    }

    @Test
    void sendContractUpdateMail_shouldRejectInvalidTenant() {
        HttpResponseMessage response = function.sendContractUpdateMail(request(VALID_BODY, null), context);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode());
        verifyNoInteractions(contractUpdateCampaignService);
    }

    @Test
    void sendContractUpdateMail_shouldReturnServerErrorWhenSendFails() {
        when(contractUpdateCampaignService.send(any())).thenThrow(new GenericOnboardingException("mail error"));

        HttpResponseMessage response = function.sendContractUpdateMail(request(VALID_BODY, "AR"), context);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode());
        assertEquals("mail error", response.getBody());
        verify(telemetryService).trackFunction(eq("SendContractUpdateMail"), any(), eq(SeverityLevel.Error),
                eq(Map.of("institutionId", "institution-id", "productIds", "prod-io,prod-pagopa",
                        "userId", "requester-id", "outcome", "ERROR")));
    }

    @SuppressWarnings("unchecked")
    private static HttpRequestMessage<Optional<String>> request(String body, String tenant) {
        HttpRequestMessage<Optional<String>> req = mock(HttpRequestMessage.class);
        doReturn(Optional.of(body)).when(req).getBody();
        doReturn(tenant == null ? Map.of(FunctionInvocationLogger.USER_ID_HEADER, "pippo")
                : Map.of("X-Tenant-Id", tenant, FunctionInvocationLogger.USER_ID_HEADER, "pippo"))
                .when(req).getHeaders();
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation ->
                new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status((HttpStatus) invocation.getArguments()[0]))
                .when(req).createResponseBuilder(any(HttpStatus.class));
        return req;
    }
}
