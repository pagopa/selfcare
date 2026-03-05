package it.pagopa.selfcare.onboarding.dto;

public class NotificationCountResult {

    private String productId;
    private long notificationCount;

    public NotificationCountResult(String productId, long notificationCount) {
        this.productId = productId;
        this.notificationCount = notificationCount;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public long getNotificationCount() {
        return notificationCount;
    }

    public void setNotificationCount(long notificationCount) {
        this.notificationCount = notificationCount;
    }
}
