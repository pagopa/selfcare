package it.pagopa.selfcare.mscore.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.mscore.api.FileStorageConnector;
import it.pagopa.selfcare.mscore.config.CoreConfig;
import it.pagopa.selfcare.mscore.model.onboarding.MailTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.mail.MailPreparationException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {


    private final CoreConfig coreConfig;
    private final SesClient sesClient;
    private final FileStorageConnector fileStorageConnector;
    private final ObjectMapper mapper;

    @Override
    public void sendDelegationUserNotification(List<String> to, String templateName, String productName, Map<String, String> mailParameters) {
        try {
            String template =  fileStorageConnector.getTemplateFile(templateName);
            MailTemplate mailTemplate = mapper.readValue(template, MailTemplate.class);
            String html = StringSubstitutor.replace(mailTemplate.getBody(), mailParameters);

            Destination destination = Destination.builder()
                    .toAddresses(to)
                    .build();

            Content content = Content.builder()
                    .data(html)
                    .build();

            Content sub = Content.builder()
                    .data(productName + ": " + mailTemplate.getSubject())
                    .build();

            Body body = Body.builder()
                    .html(content)
                    .build();

            Message msg = Message.builder()
                    .subject(sub)
                    .body(body)
                    .build();

            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(destination)
                    .message(msg)
                    .source(coreConfig.getSenderMail())
                    .build();

            sesClient.sendEmail(emailRequest);
        } catch (Exception e) {
            log.error("Error sending mail to: {} for product: {}", String.join(",", to), productName);
            throw new MailPreparationException(e);
        }
    }

}
