package it.pagopa.selfcare.mscore.api;

public interface MailNotificationConnector {

    /**
     * Create a new institutionId's MailNotification document if it does not exist, otherwise update the existing one adding the productId.
     *
     * @param institutionId the institution id
     * @param productId the product id
     * @param digitalAddress the digital address
     * @param moduleDayOfTheEpoch the module of the day
     * @return true on success, false otherwise
     */
    boolean addMailNotification(String institutionId, String productId, String digitalAddress, Integer moduleDayOfTheEpoch);

    /**
     * Remove the productId from the institutionId's MailNotification. If the productId is the last one, the MailNotification document will be deleted.
     *
     * @param institutionId the institution id
     * @param productId the product id
     * @return true on success, false otherwise
     */
    boolean removeMailNotification(String institutionId, String productId);

}
