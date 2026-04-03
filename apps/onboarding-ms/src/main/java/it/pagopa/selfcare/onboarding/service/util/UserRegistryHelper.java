package it.pagopa.selfcare.onboarding.service.util;

import static it.pagopa.selfcare.onboarding.constants.CustomError.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.controller.request.UserRequest;
import it.pagopa.selfcare.onboarding.controller.request.UserRequesterDto;
import it.pagopa.selfcare.onboarding.controller.response.UserResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import it.pagopa.selfcare.onboarding.entity.UserRequester;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.mapper.UserMapper;
import it.pagopa.selfcare.product.entity.ProductRoleInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.*;

/**
 * Helper che centralizza tutte le interazioni con la User Registry API:
 * ricerca, creazione, aggiornamento utenti e recupero UUID email.
 */
@Slf4j
@ApplicationScoped
public class UserRegistryHelper {

    public static final String USERS_FIELD_LIST = "fiscalCode,familyName,name,workContacts";
    public static final String USERS_FIELD_TAXCODE = "fiscalCode";
    static final String ID_MAIL_PREFIX = "ID_MAIL#";

    @RestClient
    @Inject
    UserApi userRegistryApi;

    @Inject
    UserMapper userMapper;

    @ConfigProperty(name = "onboarding-ms.add-user-requester.enabled")
    boolean addUserRequesterEnabled;

    /**
     * Aggiunge l'UUID email del richiedente chiamando la User Registry e,
     * se necessario, aggiornando l'utente remoto.
     */
    public Uni<Void> addUserRequester(UserRequesterDto userRequesterRequest, UserRequester userRequester) {
        log.info("Starting addUserRequester");
        if (!addUserRequesterEnabled
                || Objects.isNull(userRequesterRequest)
                || StringUtils.isBlank(userRequesterRequest.getEmail())) {
            log.info("addUserRequester skipped: feature flag disabled or requester data missing");
            return Uni.createFrom().voidItem();
        }
        return userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, userRequester.getUserRequestUid())
                .onItem().transformToUni(userResource -> {
                    Optional<String> optUuid = Optional.of(retrieveUserMailUuid(userResource, userRequesterRequest.getEmail()));
                    Optional<MutableUserFieldsDto> optFields = toUpdateUserRequest(userRequesterRequest, userResource, optUuid);
                    return optFields
                            .map(req -> userRegistryApi.updateUsingPATCH(userResource.getId().toString(), req)
                                    .replaceWith(userResource.getId()))
                            .orElse(Uni.createFrom().item(userResource.getId()))
                            .onItem().invoke(() -> optUuid.ifPresent(userRequester::setUserMailUuid));
                })
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex -> ((WebApplicationException) ex).getResponse().getStatus() != 404
                        ? Uni.createFrom().failure(ex)
                        : Uni.createFrom().nullItem())
                .replaceWithVoid();
    }

    /**
     * Cerca ogni utente nella User Registry: se trovato lo aggiorna, altrimenti lo crea.
     * Restituisce la lista degli {@link User} con id, ruolo, UUID email e product role.
     */
    public Uni<List<User>> retrieveUserResources(List<UserRequest> users, Map<PartyRole, ProductRoleInfo> roleMappings) {
        return Multi.createFrom().iterable(users)
                .onItem().transformToUni(user -> {
                    log.debug("Processing user with taxCode: {}", user.getTaxCode());
                    return userRegistryApi.searchUsingPOST(USERS_FIELD_LIST, new UserSearchDto().fiscalCode(user.getTaxCode()))
                            .onItem().transformToUni(userResource -> buildUserFromFoundResource(user, userResource, roleMappings))
                            .onFailure(WebApplicationException.class)
                            .recoverWithUni(ex -> {
                                if (((WebApplicationException) ex).getResponse().getStatus() != 404) {
                                    return Uni.createFrom().failure(ex);
                                }
                                log.debug("User not found, creating new user with taxCode: {}", user.getTaxCode());
                                return createNewUserInRegistry(user, roleMappings);
                            });
                })
                .concatenate().collect().asList();
    }

    /** Recupera la lista dei codici fiscali dei manager associati all'onboarding. */
    public Uni<List<String>> retrieveOnboardingUserFiscalCodeList(Onboarding onboarding) {
        return Multi.createFrom()
                .iterable(onboarding.getUsers().stream()
                        .filter(user -> PartyRole.MANAGER.equals(user.getRole()))
                        .map(User::getId).toList())
                .onItem().transformToUni(userId -> userRegistryApi.findByIdUsingGET(USERS_FIELD_TAXCODE, userId))
                .merge().collect().asList()
                .onItem().transform(usersResource -> usersResource.stream().map(UserResource::getFiscalCode).toList());
    }

    /** Arricchisce ogni {@link User} con i dati anagrafici provenienti dalla User Registry. */
    public Uni<List<UserResponse>> toUserResponseWithUserInfo(List<User> users) {
        return Multi.createFrom().iterable(users)
                .onItem().transformToUni(user ->
                        userRegistryApi.findByIdUsingGET(USERS_FIELD_LIST, user.getId())
                                .onItem().transform(userResource -> {
                                    UserResponse userResponse = userMapper.toUserResponse(user);
                                    userMapper.fillUserResponse(userResource, userResponse);
                                    Optional.ofNullable(userResource.getWorkContacts())
                                            .filter(map -> map.containsKey(user.getUserMailUuid()))
                                            .map(map -> map.get(user.getUserMailUuid()))
                                            .filter(wc -> StringUtils.isNotBlank(wc.getEmail().getValue()))
                                            .map(wc -> wc.getEmail().getValue())
                                            .ifPresent(userResponse::setEmail);
                                    return userResponse;
                                }))
                .merge().collect().asList();
    }

    /**
     * Restituisce l'UUID della chiave work-contact corrispondente all'email fornita.
     * Se non trovata, genera un nuovo UUID con prefisso {@code ID_MAIL#}.
     */
    public String retrieveUserMailUuid(UserResource foundUser, String userMail) {
        if (Objects.isNull(foundUser.getWorkContacts())) {
            return ID_MAIL_PREFIX.concat(UUID.randomUUID().toString());
        }
        return foundUser.getWorkContacts().entrySet().stream()
                .filter(entry -> Objects.nonNull(entry.getValue()) && Objects.nonNull(entry.getValue().getEmail()))
                .filter(entry -> entry.getValue().getEmail().getValue().equals(userMail))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse(ID_MAIL_PREFIX.concat(UUID.randomUUID().toString()));
    }

    /**
     * Costruisce il DTO di aggiornamento utente solo per i campi che differiscono
     * da quelli già presenti nella User Registry (rispettando le certificazioni).
     */
    public static Optional<MutableUserFieldsDto> toUpdateUserRequest(
            Object user, UserResource foundUser, Optional<String> optUserMailRandomUuid) {

        String name, surname, email;
        if (user instanceof UserRequest req) {
            name = req.getName(); surname = req.getSurname(); email = req.getEmail();
        } else if (user instanceof UserRequesterDto req) {
            name = req.getName(); surname = req.getSurname(); email = req.getEmail();
        } else {
            throw new IllegalArgumentException("Unsupported user type: " + user.getClass());
        }

        Optional<MutableUserFieldsDto> dto = Optional.empty();
        if (isFieldToUpdate(foundUser.getName(), name)) {
            MutableUserFieldsDto d = new MutableUserFieldsDto();
            d.setName(certifiedField(name));
            dto = Optional.of(d);
        }
        if (isFieldToUpdate(foundUser.getFamilyName(), surname)) {
            MutableUserFieldsDto d = dto.orElseGet(MutableUserFieldsDto::new);
            d.setFamilyName(certifiedField(surname));
            dto = Optional.of(d);
        }
        if (optUserMailRandomUuid.isPresent()) {
            boolean mailKeyMissing = Objects.isNull(foundUser.getWorkContacts())
                    || foundUser.getWorkContacts().keySet().stream()
                            .noneMatch(k -> k.equals(optUserMailRandomUuid.get()));
            if (mailKeyMissing) {
                MutableUserFieldsDto d = dto.orElseGet(MutableUserFieldsDto::new);
                WorkContactResource wc = new WorkContactResource();
                wc.setEmail(certifiedField(email));
                d.setWorkContacts(Map.of(optUserMailRandomUuid.get(), wc));
                dto = Optional.of(d);
            }
        }
        return dto;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Uni<User> buildUserFromFoundResource(UserRequest user, UserResource userResource,
                                                  Map<PartyRole, ProductRoleInfo> roleMappings) {
        Optional<String> optUuid = Optional.ofNullable(user.getEmail())
                .map(mail -> retrieveUserMailUuid(userResource, mail));
        Optional<MutableUserFieldsDto> optFields = toUpdateUserRequest(user, userResource, optUuid);
        return optFields
                .map(req -> userRegistryApi.updateUsingPATCH(userResource.getId().toString(), req)
                        .replaceWith(userResource.getId()))
                .orElse(Uni.createFrom().item(userResource.getId()))
                .map(id -> User.builder()
                        .id(id.toString())
                        .role(user.getRole())
                        .userMailUuid(optUuid.orElse(null))
                        .productRole(retrieveProductRole(user, roleMappings))
                        .build());
    }

    private Uni<User> createNewUserInRegistry(UserRequest user, Map<PartyRole, ProductRoleInfo> roleMappings) {
        String mailUuid = ID_MAIL_PREFIX.concat(UUID.randomUUID().toString());
        return userRegistryApi.saveUsingPATCH(buildSaveUserDto(user, mailUuid))
                .onItem().transform(userId -> User.builder()
                        .id(userId.getId().toString())
                        .role(user.getRole())
                        .userMailUuid(mailUuid)
                        .productRole(retrieveProductRole(user, roleMappings))
                        .build());
    }

    private String retrieveProductRole(UserRequest userInfo, Map<PartyRole, ProductRoleInfo> roleMappings) {
        try {
            if (Objects.isNull(roleMappings) || roleMappings.isEmpty())
                throw new IllegalArgumentException("Role mappings is required");
            ProductRoleInfo roleInfo = roleMappings.get(userInfo.getRole());
            if (Objects.isNull(roleInfo))
                throw new IllegalArgumentException(String.format(AT_LEAST_ONE_PRODUCT_ROLE_REQUIRED.getMessage(), userInfo.getRole()));
            if (Objects.isNull(roleInfo.getRoles()))
                throw new IllegalArgumentException(String.format(AT_LEAST_ONE_PRODUCT_ROLE_REQUIRED.getMessage(), userInfo.getRole()));
            if (roleInfo.getRoles().size() != 1)
                throw new IllegalArgumentException(String.format(MORE_THAN_ONE_PRODUCT_ROLE_AVAILABLE.getMessage(), userInfo.getRole()));
            return roleInfo.getRoles().get(0).getCode();
        } catch (IllegalArgumentException e) {
            throw new OnboardingNotAllowedException(e.getMessage(), DEFAULT_ERROR.getCode());
        }
    }

    private SaveUserDto buildSaveUserDto(UserRequest model, String mailUuid) {
        SaveUserDto resource = new SaveUserDto();
        resource.setFiscalCode(model.getTaxCode());
        resource.setName(certifiedField(model.getName()));
        resource.setFamilyName(certifiedField(model.getSurname()));
        if (Objects.nonNull(mailUuid)) {
            WorkContactResource contact = new WorkContactResource();
            contact.setEmail(certifiedField(model.getEmail()));
            resource.setWorkContacts(Map.of(mailUuid, contact));
        }
        return resource;
    }

    private static boolean isFieldToUpdate(CertifiableFieldResourceOfstring certifiedField, String value) {
        if (certifiedField == null) return true;
        boolean isNone = CertifiableFieldResourceOfstring.CertificationEnum.NONE.equals(certifiedField.getCertification());
        boolean isSame = isNone
                ? certifiedField.getValue().equals(value)
                : certifiedField.getValue().equalsIgnoreCase(value);
        if (isSame) return false;
        if (!isNone) throw new InvalidRequestException(USERS_UPDATE_NOT_ALLOWED.getMessage(), USERS_UPDATE_NOT_ALLOWED.getCode());
        return true;
    }

    private static CertifiableFieldResourceOfstring certifiedField(String value) {
        return new CertifiableFieldResourceOfstring()
                .value(value)
                .certification(CertifiableFieldResourceOfstring.CertificationEnum.NONE);
    }
}


