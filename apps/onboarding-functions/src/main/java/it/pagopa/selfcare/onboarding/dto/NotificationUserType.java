package it.pagopa.selfcare.onboarding.dto;

public enum NotificationUserType {
    ADD_INSTITUTE(QueueUserEvent.ADD_INSTITUTE),
    UPDATE_INSTITUTION(QueueUserEvent.UPDATE_INSTITUTION),
    ACTIVE_USER(QueueUserEvent.ACTIVE_USER),
    SUSPEND_USER(QueueUserEvent.SUSPEND_USER),
    DELETE_USER(QueueUserEvent.DELETE_USER);


    private final QueueUserEvent queueUserEvent;

    NotificationUserType(QueueUserEvent queueUserEvent) {
        this.queueUserEvent = queueUserEvent;
    }

    public static NotificationUserType getNotificationTypeFromQueueEvent(QueueUserEvent queueEvent) {
        for (NotificationUserType notificationType : NotificationUserType.values()) {
            if (notificationType.queueUserEvent == queueEvent) {
                return notificationType;
            }
        }
        return null; // Return null if no matching NotificationType is found
    }
}