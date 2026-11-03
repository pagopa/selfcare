package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.pagopa.selfcare.onboarding.context.TenantContext;
import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignRequest;
import it.pagopa.selfcare.onboarding.dto.ContractUpdateCampaignResponse;
import it.pagopa.selfcare.onboarding.service.ContractUpdateCampaignService;
import it.pagopa.selfcare.onboarding.service.FunctionInvocationLogger;
import it.pagopa.selfcare.onboarding.service.HttpJwtValidator;
import it.pagopa.selfcare.onboarding.service.TelemetryService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class ContractUpdateCampaignFunctions {

    private static final String SEND_CONTRACT_UPDATE_MAIL = "SendContractUpdateMail";

    private final ContractUpdateCampaignService contractUpdateCampaignService;
    private final ObjectMapper objectMapper;
    private final FunctionInvocationLogger functionInvocationLogger;
    private final HttpJwtValidator httpJwtValidator;
    private final TelemetryService telemetryService;

    public ContractUpdateCampaignFunctions(ContractUpdateCampaignService contractUpdateCampaignService,
                                           ObjectMapper objectMapper,
                                           FunctionInvocationLogger functionInvocationLogger,
                                           HttpJwtValidator httpJwtValidator,
                                           TelemetryService telemetryService) {
        this.contractUpdateCampaignService = contractUpdateCampaignService;
        this.objectMapper = objectMapper;
        this.functionInvocationLogger = functionInvocationLogger;
        this.httpJwtValidator = httpJwtValidator;
        this.telemetryService = telemetryService;
    }

    /**
     * Sends the contract update campaign PEC to one institution and flags its adhesions.
     * Business outcomes (SENT, ALREADY_NOTIFIED, MISSING_PEC, INVALID) are returned with 200.
     */
    @FunctionName(SEND_CONTRACT_UPDATE_MAIL)
    public HttpResponseMessage sendContractUpdateMail(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        Optional<HttpResponseMessage> authenticationError = httpJwtValidator.validate(request, context);
        if (authenticationError.isPresent()) {
            return authenticationError.get();
        }
        String userId = functionInvocationLogger.logInvocation(SEND_CONTRACT_UPDATE_MAIL, request);
        if (FunctionInvocationLogger.isMissingUserId(userId)) {
            return FunctionInvocationLogger.missingUserIdResponse(request);
        }

        ContractUpdateCampaignRequest campaignRequest;
        try {
            campaignRequest = objectMapper.readValue(request.getBody().orElse(""), ContractUpdateCampaignRequest.class);
        } catch (JsonProcessingException e) {
            return badRequest(request, context, "Malformed request body.");
        }
        if (Objects.isNull(campaignRequest) || StringUtils.isBlank(campaignRequest.institutionId())
                || Objects.isNull(campaignRequest.productIds()) || campaignRequest.productIds().isEmpty()) {
            return badRequest(request, context, "institutionId and productIds are required.");
        }

        TenantContext.Scope tenantScope;
        try {
            tenantScope = TenantContext.open(request, context);
        } catch (IllegalArgumentException exception) {
            return badRequest(request, context, "Invalid tenant context");
        }
        try (TenantContext.Scope ignored = tenantScope) {
            ContractUpdateCampaignResponse response = contractUpdateCampaignService.send(campaignRequest);
            telemetryService.trackFunction(SEND_CONTRACT_UPDATE_MAIL,
                    String.format("%s completed with outcome %s", SEND_CONTRACT_UPDATE_MAIL, response.outcome()),
                    SeverityLevel.Information,
                    telemetryProperties(campaignRequest, userId, response.outcome().name()));
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(response))
                    .build();
        } catch (Exception e) {
            String message = String.format("%s failed for institutionId %s: %s",
                    SEND_CONTRACT_UPDATE_MAIL, campaignRequest.institutionId(), e.getMessage());
            context.getLogger().severe(message);
            telemetryService.trackFunction(SEND_CONTRACT_UPDATE_MAIL, message, SeverityLevel.Error,
                    telemetryProperties(campaignRequest, userId, "ERROR"));
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(e.getMessage())
                    .build();
        }
    }

    private static Map<String, String> telemetryProperties(ContractUpdateCampaignRequest campaignRequest,
                                                           String userId, String outcome) {
        return Map.of(
                "institutionId", campaignRequest.institutionId(),
                "productIds", String.join(",", campaignRequest.productIds()),
                FunctionInvocationLogger.USER_ID_PROPERTY, userId,
                "outcome", outcome);
    }

    private static HttpResponseMessage badRequest(HttpRequestMessage<Optional<String>> request,
                                                  ExecutionContext context, String message) {
        context.getLogger().warning(message);
        return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(message).build();
    }
}
