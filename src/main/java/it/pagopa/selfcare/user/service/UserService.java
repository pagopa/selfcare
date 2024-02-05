package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.time.LocalDateTime;
import java.util.List;


public interface UserService {
    Uni<List<String>> getUsersEmails(String institutionId, String productId);

    Multi<UserProductResponse> getUserProductsByInstitution(String institutionId);

    Uni<UserResource> retrievePerson(String userId, String productId, String institutionId);

    Uni<UserInfo> retrieveBindings(String institutionId, String userId, String[] states);

    Uni<Void> updateUserStatusWithOptionalFilter(String userId, String institutionId, String productId, PartyRole role, String productRole, OnboardedProductState status);

    Multi<UserInstitutionResponse> findAllUserInstitutions(String institutionId, String userId, List<String> roles, List<String> states, List<String> products, List<String> productRoles);

    Multi<UserInstitutionResponse> findPaginatedUserInstitutions(String institutionId, String userId, List<PartyRole> roles, List<String> states, List<String> products, List<String> productRoles, Integer page, Integer size);

    Uni<Void> deleteUserInstitutionProduct(String userId, String institutionId, String productId);

    Uni<List<UserNotificationToSend>> findPaginatedUserNotificationToSend(Integer size, Integer page, String productId);

    Uni<List<UserInstitutionResponse>> findAllByIds(List<String> userIds);
    Uni<Void> updateUserProductCreatedAt(String institutionId, List<String> userIds, String productId, LocalDateTime createdAt);
}
