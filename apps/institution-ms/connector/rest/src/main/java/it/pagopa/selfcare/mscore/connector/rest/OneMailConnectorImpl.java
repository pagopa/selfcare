package it.pagopa.selfcare.mscore.connector.rest;

import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.mscore.api.OneMailConnector;
import it.pagopa.selfcare.mscore.connector.rest.client.OneMailRestClient;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailAddress;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailHighPriorityBodyDTO;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailSuccessResponseDTO;
import lombok.extern.slf4j.Slf4j;
import it.pagopa.selfcare.commons.tenant.TenantContext;
import it.pagopa.selfcare.commons.tenant.TenantRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class OneMailConnectorImpl implements OneMailConnector {

    private final OneMailRestClient restClient;
    private final TenantRegistry tenantRegistry;
    private final TenantContext tenantContext;

    public OneMailConnectorImpl(OneMailRestClient restClient,
                                TenantRegistry tenantRegistry, TenantContext tenantContext) {
        this.restClient = restClient;
        this.tenantRegistry = tenantRegistry;
        this.tenantContext = tenantContext;
    }

    @Override
    @Retry(name = "retryTimeout")
    public String sendMail(String email, String templateId, Map<String, String> templateAttributes) {
        Assert.hasText(email, "An email is required");
        Assert.hasText(templateId, "A templateId is required");
        log.debug("sendMail templateId = {}", templateId);

        String sender = tenantRegistry.oneMailCredentials(tenantContext.requiredTenantId()).sender();
        EmailHighPriorityBodyDTO request = EmailHighPriorityBodyDTO.builder()
                .from(new EmailAddress().email(sender))
                .to(new EmailAddress().email(email))
                .templateContent(Map.of(
                        "templateId", templateId,
                        "templateAttributes", templateAttributes))
                .build();

        ResponseEntity<EmailSuccessResponseDTO> response = restClient._v1EmailsSendHighPost(false, request);
        return Optional.ofNullable(response)
                .map(ResponseEntity::getBody)
                .map(EmailSuccessResponseDTO::getRequestId)
                .orElse(null);
    }
}
