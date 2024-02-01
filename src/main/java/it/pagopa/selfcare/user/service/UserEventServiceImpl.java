package it.pagopa.selfcare.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import it.pagopa.selfcare.user.constant.QueueEvent;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import it.pagopa.selfcare.user.model.notification.UserToNotify;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.time.LocalDateTime;


@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserEventServiceImpl implements UserEventService {
    public static final String ERROR_DURING_SEND_DATA_LAKE_NOTIFICATION_FOR_USER = "error during send dataLake notification for user {}";

    private final ObjectMapper objectMapper;

    @Inject
    @Channel("sc-users")
    private MutinyEmitter<String> usersEmitter;

    @Override
    public Uni<Void> sendUpdateUserNotificationToQueue(String userId, String institutionId) {
        log.trace("sendUpdateUserNotification start");
        log.debug("sendUpdateUserNotification userId = {}, institutionId = {}", userId, institutionId);
        return Uni.createFrom().item(constructUserNotificationToSend(userId, institutionId))
                .map(this::convertNotificationToJson)
                .flatMap(notification -> sendUserNotification(notification, userId));
    }

    private UserNotificationToSend constructUserNotificationToSend(String userId, String institutionId) {
        UserToNotify userToNotify = new UserToNotify();
        userToNotify.setUserId(userId);
        UserNotificationToSend notification = new UserNotificationToSend();
        String id = userToNotify.getUserId().concat(institutionId);
        notification.setId(id);
        notification.setUpdatedAt(LocalDateTime.now());
        notification.setInstitutionId(institutionId);
        notification.setEventType(QueueEvent.UPDATE);
        notification.setUser(userToNotify);
        return notification;
    }

    private String convertNotificationToJson(UserNotificationToSend notification) {
        try {
            return objectMapper.writeValueAsString(notification);
        } catch (JsonProcessingException e) {
            log.warn(ERROR_DURING_SEND_DATA_LAKE_NOTIFICATION_FOR_USER, notification.getUser().getUserId());
            throw new InvalidRequestException(ERROR_DURING_SEND_DATA_LAKE_NOTIFICATION_FOR_USER);
        }
    }

    private Uni<Void> sendUserNotification(String message, String userId) {
        return usersEmitter.sendMessage(Message.of(message))
            .onItem().invoke(() -> log.info("sent dataLake notification for user : {}", userId))
            .onFailure().invoke((throwable) -> log.warn("error during send dataLake notification for user {}: {} ", userId, throwable.getMessage(), throwable));
    }
}
