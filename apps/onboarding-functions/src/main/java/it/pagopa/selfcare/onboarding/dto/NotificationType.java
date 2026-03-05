package it.pagopa.selfcare.onboarding.dto;

public enum NotificationType {
    ADD_INSTITUTE(QueueEvent.ADD),
    UPDATE_INSTITUTION(QueueEvent.UPDATE);

    private final QueueEvent queueEvent;
    NotificationType(QueueEvent queueEvent){
        this.queueEvent = queueEvent;
    }

    public static NotificationType getNotificationTypeFromQueueEvent(QueueEvent queueEvent) {
        for (NotificationType notificationType : NotificationType.values()) {
            if (notificationType.queueEvent == queueEvent) {
                return notificationType;
            }
        }
        return null; // Return null if no matching NotificationType is found
    }
}