package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.QueueEvent;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
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
import org.openapi.quarkus.user_registry_json.api.UserApi;


@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserRegistryServiceImpl implements UserRegistryService {
    private static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";

    private final UserInstitutionService userInstitutionService;
    private final UserUtils userUtils;
    private final UserNotificationService userNotificationService;


    @RestClient
    @Inject
    private UserApi userRegistryApi;


    @Override
    public Uni<List<UserNotificationToSend>> updateUserRegistryAndSendNotificationToQueue(MutableUserFieldsDto userDto, String userId, String institutionId) {
        log.trace("sendUpdateUserNotification start");
        log.debug("sendUpdateUserNotification userId = {}, institutionId = {}", userId, institutionId);

        UserInstitutionFilter userInstitutionFilter = UserInstitutionFilter.builder()
                .userId(userId)
                .institutionId(StringUtils.isNotBlank(institutionId) ? institutionId : null).build();
        return userRegistryApi.updateUsingPATCH(userId, userDto)
                .onItem().transformToMulti(response -> userInstitutionService.findAllWithFilter(userInstitutionFilter.constructMap()))
                .onItem().transformToMultiAndMerge(userInstitution -> userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userInstitution.getUserId())
                        .onItem().transformToMulti(userResource -> Multi.createFrom().iterable(userUtils.buildUsersNotificationResponse(userInstitution, userResource, QueueEvent.UPDATE))))
                .onItem().transformToUniAndMerge(notification -> userNotificationService.sendKafkaNotification(notification, userId))
                .collect().asList();
    }
}
