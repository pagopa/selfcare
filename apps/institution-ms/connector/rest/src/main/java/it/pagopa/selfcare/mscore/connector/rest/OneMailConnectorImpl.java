package it.pagopa.selfcare.mscore.connector.rest;

import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.mscore.api.OneMailConnector;
import it.pagopa.selfcare.mscore.connector.rest.client.OneMailRestClient;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailAddress;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailHighPriorityBodyDTO;
import it.pagopa.selfcare.one_mail.generated.openapi.v1.dto.EmailSuccessResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class OneMailConnectorImpl implements OneMailConnector {

    private final OneMailRestClient restClient;
    private final String senderMail;

    public OneMailConnectorImpl(OneMailRestClient restClient,
                                @Value("${rest-client.one-mail.sender}") String senderMail) {
        this.restClient = restClient;
        this.senderMail = senderMail;
    }

    @Override
    @Retry(name = "retryTimeout")
    public String sendMail(String email, String templateId, Map<String, String> templateAttributes) {
        Assert.hasText(email, "An email is required");
        Assert.hasText(templateId, "A templateId is required");
        log.debug("sendMail templateId = {}", templateId);

        EmailHighPriorityBodyDTO request = EmailHighPriorityBodyDTO.builder()
                .from(new EmailAddress().email(senderMail))
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
