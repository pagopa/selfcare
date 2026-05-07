package it.pagopa.selfcare.dashboard.integration_test.model;

import it.pagopa.selfcare.dashboard.model.CreateUserDto;
import it.pagopa.selfcare.dashboard.model.GeographicTaxonomyListDto;
import it.pagopa.selfcare.dashboard.model.UpdateInstitutionDto;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationRequestDto;
import it.pagopa.selfcare.dashboard.model.support.SupportRequestDto;
import it.pagopa.selfcare.dashboard.model.user.UserProductRoles;
import it.pagopa.selfcare.dashboard.model.user_groups.CreateUserGroupDto;
import it.pagopa.selfcare.dashboard.model.user_groups.UpdateUserGroupDto;
import lombok.Data;

@Data
public class Requests {
    private SupportRequestDto supportRequestDto;
    private DelegationRequestDto delegationRequestDto;
    private CreateUserGroupDto createUserGroupDto;
    private UpdateUserGroupDto updateUserGroupDto;
    private GeographicTaxonomyListDto geographicTaxonomyListDto;
    private UpdateInstitutionDto updateInstitutionDto;
    private CreateUserDto createUserDto;
    private UserProductRoles userProductRoles;
}
