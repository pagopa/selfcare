package it.pagopa.selfcare.onboarding.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.pagopa.selfcare.onboarding.dto.AckPayloadRequest;
import it.pagopa.selfcare.onboarding.service.CheckOrganizationService;
import it.pagopa.selfcare.onboarding.utils.AckStatus;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static it.pagopa.selfcare.onboarding.utils.Utils.readAckPayloadValue;

public class ExternalFunctions {
    private final CheckOrganizationService checkOrganizationService;
    private final ObjectMapper objectMapper;

    public ExternalFunctions(CheckOrganizationService checkOrganizationService, ObjectMapper objectMapper) {
        this.checkOrganizationService = checkOrganizationService;
        this.objectMapper = objectMapper;
    }

    @FunctionName("TestToken")
    public HttpResponseMessage testToken(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("testToken trigger processed a request");

        String accessToken = checkOrganizationService.testToken(context);

        return request.createResponseBuilder(HttpStatus.OK).body(accessToken).build();
    }

    @FunctionName("CheckOrganization")
    public HttpResponseMessage checkOrganization(
            @HttpTrigger(name = "req", methods = {HttpMethod.HEAD}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("checkOrganization trigger processed a request");

        String fiscalCode = request.getQueryParameters().get("fiscalCode");
        String vatNumber = request.getQueryParameters().get("vatNumber");

        if (fiscalCode == null || vatNumber == null) {
            context.getLogger().warning("fiscalCode, vatNumber are required.");
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("fiscalCode, vatNumber are required.")
                    .build();
        }

        boolean alreadyRegistered = checkOrganizationService.checkOrganization(context, fiscalCode, vatNumber);

        if (alreadyRegistered) {
            return request.createResponseBuilder(HttpStatus.OK).build();
        } else {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND).build();
        }
    }

    @FunctionName("messageAcknowledgment")
    public HttpResponseMessage messageAcknowledgment(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, route = "acknowledgment/{productId}/message/{messageId}/status/{status}", authLevel = AuthorizationLevel.FUNCTION)
            HttpRequestMessage<Optional<String>> request, @BindingName("productId") String productId, @BindingName("messageId") String messageId, @BindingName("status") String status, final ExecutionContext context) {
        context.getLogger().info("messageAcknowledgment trigger processed a request");

        final AckPayloadRequest ackPayloadRequest;
        try {
            ackPayloadRequest = retrieveAckPayloadRequest(request);
        } catch (IllegalArgumentException e) {
            context.getLogger().warning(e::getMessage);
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage())
                    .build();
        }

        context.getLogger().info(() -> String.format("productId = %s, messageId = %s, status = %s, payload = %s", productId, messageId, status, ackPayloadRequest.getMessage()));
        if (AckStatus.ACK.equals(AckStatus.valueOf(status))) {
            context.getLogger().info(() -> String.format("[SUCCESSFUL Acknowledgment] - Consumer acknowledged message: %s consumption, for product = %s", messageId, productId));
        } else {
            context.getLogger().warning(() -> String.format("[ACKNOWLEDGMENT ERROR] - record with %s id gave %s, it wasn't processed correctly by %s, reason = %s", messageId, status, productId, ackPayloadRequest.getMessage()));
        }
        context.getLogger().info("messageAcknowledgment end");

        return request.createResponseBuilder(HttpStatus.OK).build();
    }

    private AckPayloadRequest retrieveAckPayloadRequest(HttpRequestMessage<Optional<String>> request) {
        String ackPayloadString = request.getBody().orElseThrow(() -> new IllegalArgumentException("Request body cannot be empty."));
        AckPayloadRequest ackPayloadRequest = readAckPayloadValue(objectMapper, ackPayloadString);
        if (StringUtils.isBlank(ackPayloadRequest.getMessage())) {
            throw new IllegalArgumentException("Field message is required in request body and cannot be blank.");
        }

        return ackPayloadRequest;
    }

  @FunctionName("webhookTest")
  public HttpResponseMessage webhookTest(
          @HttpTrigger(name = "req", methods = {HttpMethod.POST}, route = "webhookTest", authLevel = AuthorizationLevel.FUNCTION)
          HttpRequestMessage<Optional<String>> request, final ExecutionContext context) {
    context.getLogger().info("webhookTest trigger processed a request");

    try {
      String body = request.getBody().orElseThrow(() -> new IllegalArgumentException("Request body cannot be empty."));
      JsonNode jsonNode = objectMapper.readTree(body);
      context.getLogger().info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode));

    } catch (Exception e) {
      context.getLogger().warning(e::getMessage);
      return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
              .body(e.getMessage())
              .build();
    }

    context.getLogger().info("webhookTest end");

    return request.createResponseBuilder(HttpStatus.OK).build();
  }
}
