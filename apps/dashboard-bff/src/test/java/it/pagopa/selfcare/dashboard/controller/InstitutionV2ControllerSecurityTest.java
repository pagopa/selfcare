package it.pagopa.selfcare.dashboard.controller;

import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.dashboard.client.IamRestClient;
import it.pagopa.selfcare.dashboard.client.UserApiRestClient;
import it.pagopa.selfcare.dashboard.client.UserGroupRestClient;
import it.pagopa.selfcare.dashboard.config.MethodSecurityConfig;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationWithPagination;
import it.pagopa.selfcare.dashboard.model.delegation.GetDelegationParameters;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionResourceMapper;
import it.pagopa.selfcare.dashboard.model.mapper.OnboardingMapper;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapper;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperV2;
import it.pagopa.selfcare.dashboard.service.DelegationService;
import it.pagopa.selfcare.dashboard.service.InstitutionV2Service;
import it.pagopa.selfcare.dashboard.service.UserV2Service;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductWithActions;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionWithActions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(classes = {InstitutionV2Controller.class, MethodSecurityConfig.class})
@MockBean({
        InstitutionV2Service.class,
        UserV2Service.class,
        InstitutionResourceMapper.class,
        UserMapper.class,
        UserMapperV2.class,
        OnboardingMapper.class,
        UserGroupRestClient.class,
        IamRestClient.class
})
class InstitutionV2ControllerSecurityTest {

    private static final String INSTITUTION_ID = "institutionId";
    private static final String USER_ID = "userId";
    private static final String PRODUCT_ID = "prod-pagopa-pt";

    @Autowired
    private InstitutionV2Controller institutionV2Controller;
    @MockBean
    private UserApiRestClient userApiRestClient;
    @MockBean
    private DelegationService delegationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                SelfCareUser.builder(USER_ID).build(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = PRODUCT_ID)
    void getDelegationsUsingTo_withOnlyViewManagedInstitutionsPermission_shouldAllowAccess(String productId) {
        givenUserPermissions(productId, List.of("Selc:ViewManagedInstitutions"));
        GetDelegationParameters parameters = GetDelegationParameters.builder()
                .to(INSTITUTION_ID)
                .productId(productId)
                .build();
        DelegationWithPagination delegations = new DelegationWithPagination(List.of(), null);
        when(delegationService.getDelegationsV2(parameters)).thenReturn(delegations);

        ResponseEntity<DelegationWithPagination> response = institutionV2Controller.getDelegationsUsingTo(
                INSTITUTION_ID, productId, null, null, null, null);

        assertEquals(ResponseEntity.ok(delegations), response);
        verify(userApiRestClient)._getUserInstitutionWithPermission(INSTITUTION_ID, USER_ID, productId);
        verify(delegationService).getDelegationsV2(parameters);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = PRODUCT_ID)
    void getDelegationsUsingTo_withoutPermissions_shouldDenyAccess(String productId) {
        givenUserPermissions(productId, List.of());

        assertThrows(AccessDeniedException.class, () -> institutionV2Controller.getDelegationsUsingTo(
                INSTITUTION_ID, productId, null, null, null, null));

        verify(userApiRestClient)._getUserInstitutionWithPermission(INSTITUTION_ID, USER_ID, productId);
        verifyNoInteractions(delegationService);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = PRODUCT_ID)
    void getDelegationsUsingTo_withOnlyViewDelegationsPermission_shouldDenyAccess(String productId) {
        givenUserPermissions(productId, List.of("Selc:ViewDelegations"));

        assertThrows(AccessDeniedException.class, () -> institutionV2Controller.getDelegationsUsingTo(
                INSTITUTION_ID, productId, null, null, null, null));

        verify(userApiRestClient)._getUserInstitutionWithPermission(INSTITUTION_ID, USER_ID, productId);
        verifyNoInteractions(delegationService);
    }

    private void givenUserPermissions(String productId, List<String> actions) {
        OnboardedProductWithActions product = new OnboardedProductWithActions();
        product.setProductId(PRODUCT_ID);
        product.setUserProductActions(actions);
        UserInstitutionWithActions institution = new UserInstitutionWithActions();
        institution.setInstitutionId(INSTITUTION_ID);
        institution.setProducts(List.of(product));
        when(userApiRestClient._getUserInstitutionWithPermission(INSTITUTION_ID, USER_ID, productId))
                .thenReturn(ResponseEntity.ok(institution));
    }
}
