package it.pagopa.selfcare.dashboard.security;

import io.github.resilience4j.retry.annotation.Retry;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.dashboard.client.IamRestClient;
import it.pagopa.selfcare.dashboard.client.UserApiRestClient;
import it.pagopa.selfcare.dashboard.client.UserGroupRestClient;
import it.pagopa.selfcare.dashboard.model.groups.UserGroupInfo;
import it.pagopa.selfcare.dashboard.model.groups.UserGroupStatus;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.model.user.UserInfo;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.UserGroupResource;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.PermissionResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionWithActions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class SelfCarePermissionEvaluatorV2 implements PermissionEvaluator {

    public static final String GRANTED = "GRANTED";
    public static final String DENIED = "DENIED";
    private final UserGroupRestClient userGroupRestClient;
    private final UserApiRestClient userApiRestClient;
    private final IamRestClient iamRestClient;
    static final String REQUIRED_GROUP_ID_MESSAGE = "A user group id is required";
    private static final String ISSUER_PAGOPA = "PAGOPA";
    private static final String PERMISSION_ARB = "Selc:ARB";

    public SelfCarePermissionEvaluatorV2(UserGroupRestClient restClient, UserApiRestClient userApiRestClient,IamRestClient iamRestClient) {
        this.userGroupRestClient = restClient;
        this.userApiRestClient = userApiRestClient;
        this.iamRestClient = iamRestClient;
    }

    static final Function<UserGroupResource, UserGroupInfo> GROUP_RESPONSE_TO_GROUP_INFO = groupResponse -> {
        UserGroupInfo groupInfo = new UserGroupInfo();
        groupInfo.setId(groupResponse.getId());
        groupInfo.setInstitutionId(groupResponse.getInstitutionId());
        groupInfo.setProductId(groupResponse.getProductId());
        groupInfo.setName(groupResponse.getName());
        groupInfo.setDescription(groupResponse.getDescription());
        if (Objects.nonNull(groupResponse.getStatus())) {
            groupInfo.setStatus(UserGroupStatus.valueOf(groupResponse.getStatus().getValue()));
        }
        if (groupResponse.getMembers() != null) {
            List<UserInfo> members = groupResponse.getMembers().stream().map(id -> {
                UserInfo member = new UserInfo();
                member.setId(id.toString());
                return member;
            }).toList();
            groupInfo.setMembers(members);
        }
        groupInfo.setCreatedAt(groupResponse.getCreatedAt());
        groupInfo.setModifiedAt(groupResponse.getModifiedAt());
        User createdBy = new User();
        createdBy.setId(groupResponse.getCreatedBy());
        groupInfo.setCreatedBy(createdBy);
        if (groupResponse.getModifiedBy() != null) {
            User modifiedBy = new User();
            modifiedBy.setId(groupResponse.getModifiedBy());
            groupInfo.setModifiedBy(modifiedBy);
        }
        return groupInfo;
    };

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        log.info("Start check Permission");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "hasPermission authentication = {}, targetDomainObject = {}, permission = {}", authentication, targetDomainObject, permission);
        Assert.notNull(permission, "A permission type is required");

        SelfCareUser selfCareUser = (SelfCareUser) authentication.getPrincipal();
        String userId = selfCareUser.getId();
        String issuer = selfCareUser.getIssuer();
        String permissionValue = permission.toString();

        // If permission is ARB allow all users with issuer PagoPA
        if (PERMISSION_ARB.equalsIgnoreCase(permissionValue)) {
            boolean result = isPagoPaIssuer(issuer);
            log.debug("Custom permission ARB {} -> {}", permissionValue, result ? GRANTED : DENIED);
            log.trace("Permission check end (custom ARB)");
            return result;
        }

        // If issuer is PagoPA check IAM permission
        if (isPagoPaIssuer(issuer)) {
            boolean result = checkIamPermission(userId, targetDomainObject, permissionValue);
            log.debug("PAGOPA permission {} -> {}", permissionValue, result ? GRANTED : DENIED);
            log.trace("Permission check end (issuer PAGOPA)");
            return result;
        }

        // If issuer is not PagoPA check User permission
        boolean result = checkUserApiPermission(userId, targetDomainObject, permissionValue);
        log.debug("Permission {} -> {}", permissionValue, result ? GRANTED : DENIED);
        log.trace("Permission check end");

        return result;
    }

    private boolean isPagoPaIssuer(String issuer) {
        return ISSUER_PAGOPA.equalsIgnoreCase(issuer);
    }

    private boolean checkIamPermission(String userId, Object targetDomainObject, String permission) {
        FilterAuthorityDomain filterAuthorityDomain = extractFilterAuthorityDomain(targetDomainObject);

        return Optional.ofNullable(
                        iamRestClient._hasIAMUserPermission(
                                        permission,
                                        userId,
                                        filterAuthorityDomain.getInstitutionId(),
                                        filterAuthorityDomain.getProductId())
                                .getBody())
                .map(PermissionResponse::getHasPermission)
                .orElse(false);
    }

    private boolean checkUserApiPermission(String userId, Object targetDomainObject, String permission) {
        if (!(targetDomainObject instanceof FilterAuthorityDomain filterAuthorityDomain)) {
            log.warn("Target domain object is not FilterAuthorityDomain");
            return false;
        }

        if (StringUtils.hasText(filterAuthorityDomain.getGroupId())) {
            UserGroupInfo userGroupInfo = getUserGroupById(filterAuthorityDomain.getGroupId());

            if (Objects.isNull(userGroupInfo)) {
                log.warn("User group not found for groupId={}", filterAuthorityDomain.getGroupId());
                return false;
            }

            return hasPermissionOnInstitutionProduct(
                    userId,
                    userGroupInfo.getInstitutionId(),
                    userGroupInfo.getProductId(),
                    permission
            );
        }

        return hasPermissionOnInstitutionProduct(
                userId,
                filterAuthorityDomain.getInstitutionId(),
                filterAuthorityDomain.getProductId(),
                permission
        );
    }

    private FilterAuthorityDomain extractFilterAuthorityDomain(Object targetDomainObject) {
        return Optional.ofNullable(targetDomainObject)
                .filter(FilterAuthorityDomain.class::isInstance)
                .map(FilterAuthorityDomain.class::cast)
                .orElseGet(() -> new FilterAuthorityDomain(null, null, null));
    }

    private UserGroupInfo getUserGroupById(String id) {
        log.trace("getUserGroupById start");
        log.debug("getUseGroupById id = {}", id);
        Assert.hasText(id, REQUIRED_GROUP_ID_MESSAGE);
        UserGroupResource response = userGroupRestClient._getUserGroupUsingGET(id).getBody();
        UserGroupInfo groupInfo = GROUP_RESPONSE_TO_GROUP_INFO.apply(response);
        log.debug("getUseGroupById groupInfo = {}", groupInfo);
        log.trace("getUserGroupById end");
        return groupInfo;
    }

    @Retry(name = "retryTimeout")
    private Boolean hasPermissionOnInstitutionProduct(String userId, String institutionId, String productId, String action) {
        log.trace("permissionInstitutionIdPermissionGet start");
        log.debug("permissionInstitutionIdPermissionGet userId = {}, institutionId = {}, productId = {} for action = {}", userId, institutionId, productId, action);
        boolean result = false;
        UserInstitutionWithActions userInstitutionWithActions = userApiRestClient._getUserInstitutionWithPermission(institutionId, userId, productId).getBody();
        if (Objects.nonNull(userInstitutionWithActions) && !CollectionUtils.isEmpty(userInstitutionWithActions.getProducts())) {
            result = userInstitutionWithActions.getProducts().stream().anyMatch(onboardedProductWithActions -> onboardedProductWithActions.getUserProductActions().contains(action));
        }
        log.debug("permissionInstitutionIdPermissionGet result = {}", result);
        log.trace("permissionInstitutionIdPermissionGet end");
        return result;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) {
        return false;
    }

}
