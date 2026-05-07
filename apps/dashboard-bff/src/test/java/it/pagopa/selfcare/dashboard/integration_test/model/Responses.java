package it.pagopa.selfcare.dashboard.integration_test.model;

import it.pagopa.selfcare.dashboard.model.IdentityTokenResource;
import it.pagopa.selfcare.dashboard.model.InstitutionBaseResource;
import it.pagopa.selfcare.dashboard.model.InstitutionResource;
import it.pagopa.selfcare.dashboard.model.InstitutionUserDetailsResource;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationIdResource;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationResource;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationWithPagination;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.product.BrokerResource;
import it.pagopa.selfcare.dashboard.model.product.ProductRoleMappingsResource;
import it.pagopa.selfcare.dashboard.model.product.ProductUserResource;
import it.pagopa.selfcare.dashboard.model.support.SupportResponse;
import it.pagopa.selfcare.dashboard.model.user.*;
import it.pagopa.selfcare.dashboard.model.user_groups.UserGroupIdResource;
import it.pagopa.selfcare.dashboard.model.user_groups.UserGroupResource;
import lombok.Data;

import java.net.URI;
import java.util.List;

@Data
public class Responses {
    protected IdentityTokenResource identityTokenResource;
    protected SupportResponse supportResponse;
    protected URI backOfficeUrl;
    protected List<BrokerResource> brokerResource;
    protected List<ProductRoleMappingsResource> productRoleMappingsResource;
    private DelegationWithPagination delegationWithPagination;
    private List<DelegationResource> delegationResource;
    private DelegationIdResource delegationIdResource;
    private UserGroupIdResource userGroupIdResource;
    private UserGroupPlainResourcePageable userGroupPlainResourcePageable;
    private UserGroupResource userGroupResource;
    private UserResource userResource;
    private List<ProductUserResource> productUserResource;
    private List<UserInstitutionRole> userInstitutionRoles;
    private InstitutionResource institutionResource;
    private Institution institution;
    private InstitutionUserDetailsResource institutionUserDetailsResource;
    private List<InstitutionBaseResource> institutionBaseResourceList;
    private UserIdResource userIdResource;
    private UserCountResource userCountResource;
    private CheckUserResponse checkUserResponse;
    private CheckAttachmentResponse checkAttachmentResponse;
}
