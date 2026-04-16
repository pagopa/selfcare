package it.pagopa.selfcare.mscore.core;

public interface MailNotificationService {

    void sendMailForDelegation(String institutionName, String productId, String partnerId);

}
