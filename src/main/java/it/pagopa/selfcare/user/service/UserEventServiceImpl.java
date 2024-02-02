package it.pagopa.selfcare.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import it.pagopa.selfcare.user.constant.QueueEvent;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;
import java.util.ArrayList;
import org.openapi.quarkus.user_registry_json.api.UserApi;


@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserEventServiceImpl implements UserEventService {
    public static final String ERROR_DURING_SEND_DATA_LAKE_NOTIFICATION_FOR_USER = "error during send dataLake notification for user {}";
    private static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";


    private final ObjectMapper objectMapper;
    private final UserInstitutionService userInstitutionService;
    private final UserUtils userUtils;

    @RestClient
    @Inject
    private UserApi userRegistryApi;

    @Inject
    @Channel("sc-users")
    private MutinyEmitter<String> usersEmitter;

    @Override
    public Uni<Void> sendUpdateUserNotificationToQueue(MutableUserFieldsDto userDto, String userId, String institutionId) {
        log.trace("sendUpdateUserNotification start");
        log.debug("sendUpdateUserNotification userId = {}, institutionId = {}", userId, institutionId);
        UserInstitutionFilter userInstitutionFilter;
        if (StringUtils.isNotBlank(institutionId)) {
            userInstitutionFilter = UserInstitutionFilter.builder().userId(userId).institutionId(institutionId).build();
        } else {
            userInstitutionFilter = UserInstitutionFilter.builder().userId(userId).build();
        }
        return userRegistryApi.updateUsingPATCH(userId, userDto)
                .onItem().transformToMulti(response -> userInstitutionService.findAllWithFilter(userInstitutionFilter.constructMap()))
                .onItem().transformToUniAndMerge(userInstitution -> userRegistryApi
                        .findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId())
                        .map(userResource -> userUtils.buildUsersNotificationResponse(userInstitution, userResource, QueueEvent.UPDATE)))
                .collect().in(() -> new ArrayList<UserNotificationToSend>(), ArrayList::addAll)
                .onItem().transformToMulti(messages -> Multi.createFrom().iterable(messages))
                .onItem().transformToUni(notification -> sendUserNotification(notification, userId))
                .merge().toUni();
    }


    private String convertNotificationToJson(UserNotificationToSend userNotificationToSend) {
        try {
            return objectMapper.writeValueAsString(userNotificationToSend);
        } catch (JsonProcessingException e) {
            log.warn(ERROR_DURING_SEND_DATA_LAKE_NOTIFICATION_FOR_USER, userNotificationToSend.getUser().getUserId());
            throw new InvalidRequestException(ERROR_DURING_SEND_DATA_LAKE_NOTIFICATION_FOR_USER);
        }
    }

    private Uni<Void> sendUserNotification(UserNotificationToSend userNotificationToSend, String userId) {
        String message = convertNotificationToJson(userNotificationToSend);
        return usersEmitter.sendMessage(Message.of(message))
                .onItem().invoke(() -> log.info("sent dataLake notification for user : {}", userId))
                .onFailure().invoke((throwable) -> log.warn("error during send dataLake notification for user {}: {} ", userId, throwable.getMessage(), throwable));
    }
}
