package it.pagopa.selfcare.external_api.service;

import it.pagopa.selfcare.external_api.model.onboarding.OnboardedInstitutionInfo;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardedInstitutionResource;
import it.pagopa.selfcare.external_api.model.user.RelationshipState;
import it.pagopa.selfcare.external_api.model.user.UserDetailsWrapper;
import it.pagopa.selfcare.external_api.model.user.UserInfoWrapper;
import it.pagopa.selfcare.external_api.model.user.UserInstitution;
import it.pagopa.selfcare.onboarding.common.PartyRole;

import java.util.List;

public interface UserService {


    UserInfoWrapper getUserInfoV2(String fiscalCode, List<RelationshipState> userStatuses, String productId);

    List<OnboardedInstitutionInfo> getOnboardedInstitutionsDetails(String userId, String productId);

    UserDetailsWrapper getUserOnboardedProductsDetailsV2(String userId, String institutionId, String productId);

    List<OnboardedInstitutionResource> getOnboardedInstitutionsDetailsActive(String userId, String productId);

    List<UserInstitution> getUsersInstitutions(String userId, String institutionId, Integer page, Integer size, List<String> productRoles, List<String> products, List<PartyRole> roles, List<String> states);
}
