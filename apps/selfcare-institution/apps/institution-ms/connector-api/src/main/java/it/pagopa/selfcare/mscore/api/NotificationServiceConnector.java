package it.pagopa.selfcare.mscore.api;


import it.pagopa.selfcare.mscore.model.notification.MessageRequest;
import it.pagopa.selfcare.mscore.model.notification.MultipleReceiverMessageRequest;

public interface NotificationServiceConnector {
    void sendNotificationToUser(MessageRequest messageRequest);
    void sendNotificationToUsers(MultipleReceiverMessageRequest multipleReceiverMessageRequest);

}
