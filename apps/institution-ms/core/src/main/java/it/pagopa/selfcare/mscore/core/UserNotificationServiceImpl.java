package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.OneMailConnector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final String DELEGATION_USER_TEMPLATE_ID = "selfcare_delegation_user_notification";

    private final OneMailConnector oneMailConnector;

    @Override
    public void sendDelegationUserNotification(List<String> to, Map<String, String> templateAttributes) {
        // OneMail accepts a single recipient per request
        for (String email : to) {
            try {
                String requestId = oneMailConnector.sendMail(email, DELEGATION_USER_TEMPLATE_ID, templateAttributes);
                log.info("Delegation user notification sent to: {} with templateId: {}, requestId: {}",
                        email, DELEGATION_USER_TEMPLATE_ID, requestId);
            } catch (Exception e) {
                log.error("Error sending delegation user notification to: {} with templateId: {}",
                        email, DELEGATION_USER_TEMPLATE_ID, e);
            }
        }
    }

}
