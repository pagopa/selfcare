package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.controller.request.AddUserRoleDto;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.controller.response.*;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.UserNotificationToSend;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import it.pagopa.selfcare.user.service.utils.CreateOrUpdateUserByFiscalCodeResponse;

import java.time.OffsetDateTime;
import java.util.List;


public interface UserService {
    Uni<List<String>> getUsersEmails(String institutionId, String productId);

    Multi<UserProductResponse> getUserProductsByInstitution(String institutionId);

    Uni<UserResponse> retrievePerson(String userId, String productId, String institutionId);

    Uni<UserInfo> retrieveBindings(String institutionId, String userId, String[] states);

    Uni<Void> updateUserStatusWithOptionalFilter(String userId, String institutionId, String productId, PartyRole role, String productRole, OnboardedProductState status);

    Multi<UserInstitutionResponse> findAllUserInstitutions(String institutionId, String userId, List<String> roles, List<String> states, List<String> products, List<String> productRoles);

    Multi<UserInstitutionResponse> findPaginatedUserInstitutions(String institutionId, String userId, List<PartyRole> roles, List<String> states, List<String> products, List<String> productRoles, Integer page, Integer size);

    Uni<Void> deleteUserInstitutionProduct(String userId, String institutionId, String productId);

    Uni<DeletedUserCountResponse> deleteUserInstitutionProductUsers(String institutionId, String productId);

    Uni<List<UserNotificationToSend>> findPaginatedUserNotificationToSend(Integer size, Integer page, String productId);

    Uni<List<UserInstitutionResponse>> findAllByIds(List<String> userIds);

    Uni<Void> updateUserProductCreatedAt(String institutionId, List<String> userIds, String productId, OffsetDateTime createdAt);

    Uni<UserDetailResponse> getUserById(String userId, String institutionId, String fieldsToRetrieve);

    Uni<UserDetailResponse> searchUserByFiscalCode(String fiscalCode, String institutionId);

    Uni<Void> updateUserProductStatus(String userId, String institutionId, String productId, OnboardedProductState status, String productRole, LoggedUser loggedUser);

    Uni<CreateOrUpdateUserByFiscalCodeResponse> createOrUpdateUserByFiscalCode(CreateUserDto userDto, LoggedUser loggedUser);

    Uni<String> createOrUpdateUserByUserId(AddUserRoleDto userDto, String userId, LoggedUser loggedUser, OnboardedProductState status);

    Uni<String> createUserByUserId(AddUserRoleDto userDto, String userId, LoggedUser loggedUser);

    Multi<UserDataResponse> retrieveUsersData(String institutionId, String personId, List<String> roles, List<String> states, List<String> products, List<String> productRoles, String userId);

    Uni<Void> updateInstitutionDescription(String institutionId, UpdateDescriptionDto descriptionDto);

    Uni<Void> sendEventsByDateAndUserIdAndInstitutionId(OffsetDateTime fromDate, String institutionId, String userId);

    Uni<UserInstitutionWithActions> getUserInstitutionWithPermission(String userId, String institutionId, String productId);

    Uni<UsersCountResponse> getUsersCount(String institutionId, String productId, List<PartyRole> roles, List<OnboardedProductState> status);

    Uni<Boolean> checkUser(String fiscalCode, String institutionId, String productId);

    Uni<Void> sendMail(String userId, String userMailUuid, String institutionName, String productId, PartyRole productRole, LoggedUser loggedUser);

    Uni<Void> sendEmailOtp(String userId, String institutionalEmail, String otp);
}
