package it.pagopa.selfcare.dashboard.security;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.dashboard.client.IamRestClient;
import it.pagopa.selfcare.dashboard.client.UserApiRestClient;
import it.pagopa.selfcare.dashboard.client.UserGroupRestClient;
import it.pagopa.selfcare.group.generated.openapi.v1.dto.UserGroupResource;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.PermissionResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductWithActions;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionWithActions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelfCarePermissionEvaluatorV2Test {

    @Mock
    UserGroupRestClient userGroupRestClient;
    @Mock
    UserApiRestClient userApiRestClient;
    @Mock
    IamRestClient iamRestClient;
    @InjectMocks
    SelfCarePermissionEvaluatorV2 permissionEvaluator;

    @Test
    void hasPermissionReturnsTrueForValidUserGroupPermission() {
        Authentication authentication = mock(Authentication.class);
        SelfCareUser user = SelfCareUser.builder("userId").build();

        UserGroupResource userGroupResource = new UserGroupResource();
        userGroupResource.setInstitutionId("institutionId");
        userGroupResource.setProductId("productId");

        OnboardedProductWithActions onboardedProductWithActions = new OnboardedProductWithActions();
        onboardedProductWithActions.setProductId("productId");
        onboardedProductWithActions.addUserProductActionsItem("Selc:ViewBilling");

        UserInstitutionWithActions userInstitutionWithActions = new UserInstitutionWithActions();
        userInstitutionWithActions.setInstitutionId("institution");
        userInstitutionWithActions.setProducts(List.of(onboardedProductWithActions));

        when(authentication.getPrincipal()).thenReturn(user);
        when(userGroupRestClient._getUserGroupUsingGET("groupId")).thenReturn(ResponseEntity.of(Optional.of(userGroupResource)));
        when(userApiRestClient._getUserInstitutionWithPermission("institutionId", "userId", "productId")).thenReturn(ResponseEntity.ok(userInstitutionWithActions));

        assertTrue(permissionEvaluator.hasPermission(authentication, new FilterAuthorityDomain("institutionId", "productId", "groupId"), "Selc:ViewBilling"));
    }

    @Test
    void hasPermissionReturnsFalseForInvalidUserGroupPermission() {
        Authentication authentication = mock(Authentication.class);
        SelfCareUser user = SelfCareUser.builder("userId").build();

        UserInstitutionWithActions userInstitutionWithActions = new UserInstitutionWithActions();
        userInstitutionWithActions.setInstitutionId("institutionId");
        OnboardedProductWithActions onboardedProductWithActions = new OnboardedProductWithActions();
        onboardedProductWithActions.setProductId("productId");
        onboardedProductWithActions.addUserProductActionsItem("Selc:ViewBilling");
        userInstitutionWithActions.setProducts(List.of(onboardedProductWithActions));

        when(authentication.getPrincipal()).thenReturn(user);
        when(userApiRestClient._getUserInstitutionWithPermission("institutionId", "userId", "productId")).thenReturn(ResponseEntity.ok(userInstitutionWithActions));

        assertTrue(permissionEvaluator.hasPermission(authentication, new FilterAuthorityDomain("institutionId", "productId", null), "Selc:ViewBilling"));
    }

    @Test
    void hasPermissionReturnsFalseForInvalidDirectPermission() {
        Authentication authentication = mock(Authentication.class);
        SelfCareUser user = SelfCareUser.builder("userId").build();

        UserInstitutionWithActions userInstitutionWithActions = new UserInstitutionWithActions();
        userInstitutionWithActions.setInstitutionId("institutionId");

        when(authentication.getPrincipal()).thenReturn(user);
        when(userApiRestClient._getUserInstitutionWithPermission("institutionId", "userId", "productId")).thenReturn(ResponseEntity.ok(userInstitutionWithActions));

        assertFalse(permissionEvaluator.hasPermission(authentication, new FilterAuthorityDomain("institutionId", "productId", null), "Selc:ViewBilling"));
    }

    @Test
    void hasPermissionReturnsTrueForIssuerPagoPA() {
        Authentication authentication = mock(Authentication.class);
        SelfCareUser user = SelfCareUser.builder("userId").issuer("PAGOPA").build();
        FilterAuthorityDomain domain = new FilterAuthorityDomain("institutionId", "productId", null);
        String permission = "Selc:AccessProductBackofficeAdmin";

        when(authentication.getPrincipal()).thenReturn(user);
        when(iamRestClient._hasIAMUserPermission(permission, user.getId(), domain.getInstitutionId(), domain.getProductId()))
                .thenReturn(ResponseEntity.ok(new PermissionResponse(true)));

        assertTrue(permissionEvaluator.hasPermission(authentication, domain, permission));
    }

    @Test
    void hasPermissionReturnsFalseForIssuerPagoPA() {
        Authentication authentication = mock(Authentication.class);
        SelfCareUser user = SelfCareUser.builder("userId").issuer("PAGOPA").build();
        FilterAuthorityDomain domain = new FilterAuthorityDomain("institutionId", null, null);
        String permission = "Selc:ViewBilling";

        when(authentication.getPrincipal()).thenReturn(user);
        when(iamRestClient._hasIAMUserPermission(permission,  user.getId(), domain.getInstitutionId(), domain.getProductId()))
                .thenReturn(ResponseEntity.ok(new PermissionResponse(false)));

        assertFalse(permissionEvaluator.hasPermission(authentication, domain, permission));
    }

    @Test
    void hasPermissionReturnsFalseForARBWhenIssuerIsNotPagoPA() {
        Authentication authentication = mock(Authentication.class);
        SelfCareUser user = SelfCareUser.builder("userId")
                .issuer("NO_PAGOPA")
                .build();

        when(authentication.getPrincipal()).thenReturn(user);

        assertFalse(permissionEvaluator.hasPermission(
                authentication,
                null,
                "Selc:ARB"
        ));
    }

    @Test
    void hasPermissionReturnsTrueForARBWhenIssuerIsPagoPA() {
        Authentication authentication = mock(Authentication.class);
        SelfCareUser user = SelfCareUser.builder("userId")
                .issuer("PAGOPA")
                .build();

        when(authentication.getPrincipal()).thenReturn(user);

        assertTrue(permissionEvaluator.hasPermission(
                authentication,
                null,
                "Selc:ARB"
        ));
    }
}
