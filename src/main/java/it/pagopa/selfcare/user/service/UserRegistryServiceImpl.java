package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.entity.UserInstitution;
import it.pagopa.selfcare.user.entity.filter.UserInstitutionFilter;
import it.pagopa.selfcare.user.mapper.UserMapper;
import it.pagopa.selfcare.user.model.UpdateUserRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.gradle.internal.impldep.org.apache.commons.lang.StringUtils;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static it.pagopa.selfcare.user.constant.CollectionUtil.MAIL_ID_PREFIX;


@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserRegistryServiceImpl implements UserRegistryService {
    private static final String USERS_FIELD_LIST_WITHOUT_FISCAL_CODE = "name,familyName,email,workContacts";

    private final UserInstitutionService userInstitutionService;
    private final UserMapper userMapper;

    @ConfigProperty(name = "user-ms.retry.min-backoff")
    Integer retryMinBackOff;

    @ConfigProperty(name = "user-ms.retry.max-backoff")
    Integer retryMaxBackOff;

    @ConfigProperty(name = "user-ms.retry")
    Integer maxRetry;


    @RestClient
    @Inject
    UserApi userRegistryApi;

    @Override
    public Uni<UserResource> findByIdUsingGET(String fl, String id) {
        return userRegistryApi.findByIdUsingGET(fl, id)
                .onFailure(this::checkIfIsRetryableException)
                    .retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry);

    }

    @Override
    public Uni<UserId> saveUsingPATCH(SaveUserDto saveUserDto) {
        return userRegistryApi.saveUsingPATCH(saveUserDto)
                .onFailure(this::checkIfIsRetryableException)
                .retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry);
    }

    @Override
    public Uni<UserResource> searchUsingPOST(String fl, UserSearchDto userSearchDto) {
        return userRegistryApi.searchUsingPOST(fl, userSearchDto)
                .onFailure(this::checkIfIsRetryableException)
                .retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry);
    }

    @Override
    public Uni<Response> updateUsingPATCH(String id, MutableUserFieldsDto mutableUserFieldsDto) {
        return userRegistryApi.updateUsingPATCH(id, mutableUserFieldsDto)
                .onFailure(this::checkIfIsRetryableException)
                .retry().withBackOff(Duration.ofSeconds(retryMinBackOff), Duration.ofSeconds(retryMaxBackOff)).atMost(maxRetry);
    }

    private boolean checkIfIsRetryableException(Throwable throwable) {
        return throwable instanceof TimeoutException ||
                (throwable instanceof WebApplicationException webApplicationException && webApplicationException.getResponse().getStatus() == 429);
    }


    @Override
    public Uni<List<UserInstitution>> updateUserRegistry(UpdateUserRequest updateUserRequest, String userId, String institutionId) {
        log.trace("sendUpdateUserNotification start");
        log.debug("sendUpdateUserNotification userId = {}, institutionId = {}", userId, institutionId);

        if(StringUtils.isBlank(updateUserRequest.getEmail()))
            throw new IllegalArgumentException("email updateUserRequest must not be null!");

        UserInstitutionFilter userInstitutionFilter = UserInstitutionFilter.builder()
                .userId(userId)
                .institutionId(StringUtils.isNotBlank(institutionId) ? institutionId : null).build();

        return Uni.combine().all()
                .unis(userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST_WITHOUT_FISCAL_CODE, userId)
                                .onItem().ifNotNull().invoke(() -> log.debug("User founded on userRegistry with userId: {}", userId)),
                        userInstitutionService.findAllWithFilter(userInstitutionFilter.constructMap()).collect().asList()
                                .onItem().ifNotNull().invoke(() -> log.debug("UserInstitution founded for userId: {} and institutionId: {}", userId, institutionId)))
                .asTuple()
                .onItem().transformToMulti(tuple -> findMailUuidAndUpdateUserRegistry(tuple.getItem1(), updateUserRequest)
                        .onItem().transformToMulti(uuidMail -> updateUserInstitution(tuple.getItem2(), uuidMail)))
                .collect().asList()
                .onItem().invoke(items -> log.trace("update {} users on userRegistry", items.size()));
    }

    private Multi<UserInstitution> updateUserInstitution(List<UserInstitution> userInstitutions, String mailUuid) {
        return Multi.createFrom().iterable(userInstitutions.stream()
                        .peek(userInstitution -> {
                            userInstitution.setUserMailUuid(mailUuid);
                            userInstitution.setUserMailUpdatedAt(OffsetDateTime.now());
                        })
                        .toList())
                .onItem().transformToUniAndMerge(userInstitutionService::persistOrUpdate)
                .onItem().invoke(() -> log.debug("UserInstitution updated successfully"))
                .onFailure().invoke(throwable -> log.warn("Something went wrong while updating userInstitution"));
    }

    private Uni<String> findMailUuidAndUpdateUserRegistry(UserResource userResource, UpdateUserRequest userDto) {
        Optional<String> mailAlreadyPresent = Optional.empty();
        String idMail = MAIL_ID_PREFIX + UUID.randomUUID();

        if(Objects.nonNull(userResource.getWorkContacts())) {
            mailAlreadyPresent = userResource.getWorkContacts().entrySet().stream()
                    .filter(entry -> entry.getValue() != null && entry.getValue().getEmail() != null && StringUtils.isNotBlank(entry.getValue().getEmail().getValue())
                            && entry.getValue().getEmail().getValue().equalsIgnoreCase(userDto.getEmail()) && entry.getKey().startsWith(MAIL_ID_PREFIX))
                    .findFirst()
                    .map(Map.Entry::getKey);
        }

        return updateUsingPATCH(userResource.getId().toString(),
                userMapper.toMutableUserFieldsDto(userDto, userResource, mailAlreadyPresent.isPresent() ? null : idMail))
            .replaceWith(mailAlreadyPresent.orElse(idMail));
    }
}
