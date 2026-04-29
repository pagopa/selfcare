package it.pagopa.selfcare.mscore.core;

import java.util.List;
import java.util.Map;

public interface UserNotificationService {

    void sendDelegationUserNotification(List<String> to, String templateName, String productName, Map<String, String> mailParameters);

}
