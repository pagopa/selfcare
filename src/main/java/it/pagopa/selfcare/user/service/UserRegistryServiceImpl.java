package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import it.pagopa.selfcare.user.constant.QueueEvent;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.model.UpdateUserRequest;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import it.pagopa.selfcare.user.util.UserUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Flow;

import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.UserResource;


@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserRegistryServiceImpl implements UserRegistryService {
    private static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";

    private final UserInstitutionService userInstitutionService;
    private final UserUtils userUtils;
    private final UserNotificationService userNotificationService;
    private final UserMapper userMapper;


    @RestClient
    @Inject
    private UserApi userRegistryApi;


    @Override
    public Uni<List<UserNotificationToSend>> updateUserRegistryAndSendNotificationToQueue(UpdateUserRequest updateUserRequest, String userId, String institutionId) {
        log.trace("sendUpdateUserNotification start");
        log.debug("sendUpdateUserNotification userId = {}, institutionId = {}", userId, institutionId);
        UserInstitutionFilter userInstitutionFilter = UserInstitutionFilter.builder()
                .userId(userId)
                .institutionId(StringUtils.isNotBlank(institutionId) ? institutionId : null).build();

        return Uni.combine().all()
                .unis(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userId),
                        userInstitutionService.findAllWithFilter(userInstitutionFilter.constructMap()).collect().asList())
                .asTuple()
                .onItem().transformToMulti(tuple -> checkEmail(tuple.getItem1(), updateUserRequest)
                        .onItem().transformToMulti(idMail -> updateUserInstitutionAndSendNotification(tuple, idMail, userId)))
                .collect().asList();
    }

    private Multi<UserNotificationToSend> updateUserInstitutionAndSendNotification(Tuple2<UserResource, List<UserInstitution>> tuple, String idMail, String userId) {
        return Multi.createFrom().iterable(tuple.getItem2().stream()
                        .peek(userInstitution -> userInstitution.setUserMailUuid(idMail))
                        .toList())
                .onItem().transformToUniAndMerge(userInstitutionService::persistOrUpdate)
                .onItem().transformToMultiAndMerge(userInstitution -> sendKafkaNotification(tuple.getItem1(), userId, userInstitution));
    }

    private Multi<UserNotificationToSend> sendKafkaNotification(UserResource userResource, String userId, UserInstitution userInstitution) {
        return Multi.createFrom().iterable(userUtils.buildUsersNotificationResponse(userInstitution, userResource, QueueEvent.UPDATE))
                .onItem().transformToUniAndMerge(userNotificationToSend -> userNotificationService.sendKafkaNotification(userNotificationToSend, userId));
    }

    private Uni<String> checkEmail(UserResource userResource, UpdateUserRequest userDto) {
        return userResource.getWorkContacts().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().getEmail() != null && StringUtils.isNotBlank(entry.getValue().getEmail().getValue())
                        && entry.getValue().getEmail().getValue().equalsIgnoreCase(userDto.getEmail()) && entry.getKey().startsWith("ID_MAIL#"))
                .findFirst()
                .map(entry -> {
                    log.debug("Email already present in the user registry");
                    return userRegistryApi.updateUsingPATCH(userResource.getId().toString(), userMapper.toMutableUserFieldsDto(userDto, null)).replaceWith(entry.getKey());
                })
                .orElseGet(() -> {
                    String idMail = "ID_MAIL#" + UUID.randomUUID();
                    return userRegistryApi.updateUsingPATCH(userResource.getId().toString(), userMapper.toMutableUserFieldsDto(userDto, idMail)).replaceWith(idMail);
                });

    }
}
