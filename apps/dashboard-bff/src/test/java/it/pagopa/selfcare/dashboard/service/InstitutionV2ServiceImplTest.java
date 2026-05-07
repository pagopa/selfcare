package it.pagopa.selfcare.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.commons.base.security.ProductGrantedAuthority;
import it.pagopa.selfcare.commons.base.security.SelfCareGrantedAuthority;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingsResponse;
import it.pagopa.selfcare.dashboard.client.*;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperImpl;
import it.pagopa.selfcare.dashboard.model.product.mapper.ProductMapper;
import it.pagopa.selfcare.dashboard.model.user.UserInfo;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissions;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissionsList;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGet;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGetResponse;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserDataResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionWithActions;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserProductResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.commons.base.security.PartyRole.MANAGER;
import static it.pagopa.selfcare.commons.base.security.PartyRole.OPERATOR;
import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.PENDING;
import static it.pagopa.selfcare.onboarding.common.OnboardingStatus.TOBEVALIDATED;
import static it.pagopa.selfcare.onboarding.common.ProductId.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class})
class InstitutionV2ServiceImplTest extends BaseServiceTest {

    @InjectMocks
    private InstitutionV2ServiceImpl institutionV2Service;
    @Mock
    private UserApiRestClient userApiRestClient;
    @Mock
    private CoreInstitutionApiRestClient coreInstitutionApiRestClient;
    @Mock
    private OnboardingRestClient onboardingRestClient;
    @Mock
    private IamExternalRestClient iamExternalRestClient;
    @Mock
    private ProductService productService;
    @Mock
    private DocumentContentRestClient documentContentRestClient;
    @Mock
    private DocumentRestClient documentRestClient;
    @Spy
    private InstitutionMapperImpl institutionMapper;
    @Spy
    private ProductMapper productMapper;
    @Spy
    private UserMapperImpl userMapper;
    @Mock
    private UserV2ServiceImpl userV2Service;

    @BeforeEach
    void init() {
        super.setUp();
    }

    @Test
    void getInstitutionUserWithoutInstitutionId() {
        String userId = "userId";
        String loggedUserId = "loggedUserId";
        assertThrows(IllegalArgumentException.class, () -> institutionV2Service.getInstitutionUser(null, userId, loggedUserId));
    }

    @Test
    void getInstitutionUserWithoutUserId() {
        String institutionId = "institutionId";
        String loggedUserId = "loggedUserId";
        assertThrows(IllegalArgumentException.class, () -> institutionV2Service.getInstitutionUser(institutionId, null, loggedUserId));
    }

    @Test
    void getInstitutionUserNoUserFound() {
        String institutionId = "institutionId";
        String userId = "userId";
        String loggedUserId = "loggedUserId";

        UserInfo.UserInfoFilter userInfoFilter = new UserInfo.UserInfoFilter();
        userInfoFilter.setUserId(userId);

        when(userV2Service.getUsers(institutionId, userInfoFilter, loggedUserId)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () -> institutionV2Service.getInstitutionUser(institutionId, userId, loggedUserId));
    }

    @Test
    void getInstitutionUser() throws IOException {
        String institutionId = "institutionId";
        String userId = "userId";
        String loggedUserId = "loggedUserId";

        UserInfo.UserInfoFilter userInfoFilter = new UserInfo.UserInfoFilter();
        userInfoFilter.setUserId(userId);

        ClassPathResource resource = new ClassPathResource("stubs/UserInfo.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        List<UserInfo> userInfo = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        UserDataResponse userData = new UserDataResponse();
        userData.setUserId("123e4567-e89b-12d3-a456-426614174000");
        userData.setRole("MANAGER");

        when(userV2Service.getUsers(institutionId, userInfoFilter, loggedUserId)).thenReturn(userInfo);

        UserInfo actualUserInfo = institutionV2Service.getInstitutionUser("institutionId", "userId", "loggedUserId");
        assertEquals(userInfo.get(0).getId(), actualUserInfo.getId());
        verify(userV2Service, Mockito.times(1)).getUsers(institutionId, userInfoFilter, loggedUserId);
    }

    @Test
    void getAllInstitutionUser() {
        final String institutionId = "institutionId";
        final String userId = "userId";
        final String loggedUserId = "loggedUserId";

        final UserProductResponse user = new UserProductResponse();
        user.setId(userId);
        final OnboardedProductResponse product1 = new OnboardedProductResponse();
        product1.setProductId("prod-1");
        user.addProductsItem(product1);
        final OnboardedProductResponse product2 = new OnboardedProductResponse();
        product2.setProductId("prod-2");
        user.addProductsItem(product2);
        final List<UserProductResponse> allUsers = List.of(user, new UserProductResponse());
        when(userV2Service.getAllUsers(any(), any())).thenReturn(allUsers);

        final ProductRolePermissionsList iamPermissions = new ProductRolePermissionsList();
        final ProductRolePermissions permission1 = new ProductRolePermissions();
        permission1.setProductId("prod-2");
        final ProductRolePermissions permission2 = new ProductRolePermissions();
        permission2.setProductId("prod-3");
        iamPermissions.setItems(List.of(permission1, permission2));
        when(iamExternalRestClient._getIAMProductRolePermissionsList(any(), any())).thenReturn(ResponseEntity.ok(iamPermissions));

        final UserProductResponse response = institutionV2Service.getAllInstitutionUser(institutionId, userId, loggedUserId);
        assertEquals(1, response.getProducts().size());
        assertEquals("prod-2", response.getProducts().get(0).getProductId());
    }

    @Test
    void getAllInstitutionUser_withALL() {
        final String institutionId = "institutionId";
        final String userId = "userId";
        final String loggedUserId = "loggedUserId";

        final UserProductResponse user = new UserProductResponse();
        user.setId(userId);
        final OnboardedProductResponse product1 = new OnboardedProductResponse();
        product1.setProductId("prod-1");
        user.addProductsItem(product1);
        final OnboardedProductResponse product2 = new OnboardedProductResponse();
        product2.setProductId("prod-2");
        user.addProductsItem(product2);
        final List<UserProductResponse> allUsers = List.of(user, new UserProductResponse());
        when(userV2Service.getAllUsers(any(), any())).thenReturn(allUsers);

        final ProductRolePermissionsList iamPermissions = new ProductRolePermissionsList();
        final ProductRolePermissions permission1 = new ProductRolePermissions();
        permission1.setProductId("prod-2");
        final ProductRolePermissions permission2 = new ProductRolePermissions();
        permission2.setProductId("ALL");
        final ProductRolePermissions permission3 = new ProductRolePermissions();
        permission3.setProductId("prod-3");
        iamPermissions.setItems(List.of(permission1, permission2, permission3));
        when(iamExternalRestClient._getIAMProductRolePermissionsList(any(), any())).thenReturn(ResponseEntity.ok(iamPermissions));

        final UserProductResponse response = institutionV2Service.getAllInstitutionUser(institutionId, userId, loggedUserId);
        assertEquals(2, response.getProducts().size());
        assertEquals("prod-1", response.getProducts().get(0).getProductId());
        assertEquals("prod-2", response.getProducts().get(1).getProductId());
    }

    @Test
    void getAllInstitutionUser_noUser() {
        when(userV2Service.getAllUsers(any(), any())).thenReturn(List.of());
        assertThrows(ResourceNotFoundException.class, () -> institutionV2Service.getAllInstitutionUser("institutionId", "userId", "loggedUserId"));
    }

    @Test
    void getAllInstitutionUser_noIamPermissions() {
        when(userV2Service.getAllUsers(any(), any())).thenReturn(List.of(new UserProductResponse()));
        when(iamExternalRestClient._getIAMProductRolePermissionsList(any(), any())).thenReturn(ResponseEntity.ok(new ProductRolePermissionsList().items(List.of())));
        assertThrows(AccessDeniedException.class, () -> institutionV2Service.getAllInstitutionUser("institutionId", "userId", "loggedUserId"));
    }

    @Test
    void findInstitutionById() throws IOException {
        String institutionId = "institutionId";
        String userId = "userId";
        String tokenId = "id1";

        ProductGrantedAuthority productGrantedAuthority = new ProductGrantedAuthority(MANAGER, "productRole", "productId");
        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getId()).thenReturn(userId);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SelfCareGrantedAuthority(institutionId, Collections.singleton(productGrantedAuthority))));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ClassPathResource userInstitutionResource = new ClassPathResource("expectations/UserInstitutionWithActions.json");
        byte[] userInstitutionStream = Files.readAllBytes(userInstitutionResource.getFile().toPath());
        UserInstitutionWithActions userInstitutionWithActionsDto = objectMapper.readValue(userInstitutionStream, new TypeReference<>() {
        });


        ClassPathResource resource = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        InstitutionResponse institution = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        when(userApiRestClient._getUserInstitutionWithPermission(institutionId, userId, null))
                .thenReturn(ResponseEntity.ok(userInstitutionWithActionsDto));
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(institution));

        Institution result = institutionV2Service.findInstitutionById(institutionId);
        Assertions.assertNotNull(result.getOnboarding().get(0).getUserProductActions());
        Assertions.assertEquals(2, result.getOnboarding().get(0).getUserProductActions().size());
        Assertions.assertNotNull(result.getOnboarding().get(0).getCreatedAt());
        Assertions.assertEquals(tokenId, result.getOnboarding().get(0).getTokenId());
        verify(userApiRestClient, Mockito.times(1))
                ._getUserInstitutionWithPermission(institutionId, userId, null);
        verify(coreInstitutionApiRestClient, Mockito.times(1))
                ._retrieveInstitutionByIdUsingGET(institutionId, null);
    }

    @Test
    void findAllInstitutionById() throws IOException {
        // given
        String institutionId = "institutionId";
        String userId = "userId";

        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getId()).thenReturn(userId);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Stub InstitutionResponse
        ClassPathResource resource = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        InstitutionResponse institutionResponse = objectMapper.readValue(resourceStream, new TypeReference<>() {});

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(institutionResponse));

        // Stub IAM response
        ProductRolePermissions perm = ProductRolePermissions.builder()
                .productId("productId")
                .role("SUPPORT")
                .permissions(List.of("read", "write"))
                .build();

        ProductRolePermissionsList iamPermissions = ProductRolePermissionsList.builder()
                .items(List.of(perm))
                .build();

        when(iamExternalRestClient._getIAMProductRolePermissionsList(userId, null))
                .thenReturn(ResponseEntity.ok(iamPermissions));

        // when
        Institution result = institutionV2Service.findAllInstitutionById(institutionId);

        // then
        assertNotNull(result);
        assertNotNull(result.getOnboarding());
        assertFalse(result.getOnboarding().isEmpty());

        // Find the product
        var product = result.getOnboarding().stream()
                .filter(p -> "productId".equals(p.getProductId()))
                .findFirst()
                .orElseThrow();

        assertTrue(product.isAuthorized());
        assertEquals("SUPPORT", product.getUserRole());
        assertEquals(List.of("read", "write"), product.getUserProductActions());

        // Checks
        verify(userApiRestClient, never())._getUserInstitutionWithPermission(any(), any(), any());
        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
        verify(iamExternalRestClient, times(1))._getIAMProductRolePermissionsList(userId, null);
    }

    @Test
    void findInstitutionById_noIamPermissions() throws IOException {
        // given
        String institutionId = "institutionId";
        String userId = "userId";

        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getId()).thenReturn(userId);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Stub InstitutionResponse
        ClassPathResource resource = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        InstitutionResponse institutionResponse = objectMapper.readValue(resourceStream, new TypeReference<>() {});

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(institutionResponse));

        // Stub IAM response
        ProductRolePermissionsList iamPermissions = ProductRolePermissionsList.builder()
                .items(Collections.emptyList())
                .build();

        when(iamExternalRestClient._getIAMProductRolePermissionsList(userId, null))
                .thenReturn(ResponseEntity.ok(iamPermissions));

        // verify
        assertDoesNotThrow(() -> institutionV2Service.findAllInstitutionById(institutionId));
    }

    @Test
    void findAllInstitutionById_allIamPermissions() throws IOException {
        // given
        String institutionId = "institutionId";
        String userId = "userId";

        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getId()).thenReturn(userId);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Stub InstitutionResponse
        ClassPathResource resource = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        InstitutionResponse institutionResponse = objectMapper.readValue(resourceStream, new TypeReference<>() {});

        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(institutionResponse));

        // Stub IAM response
        ProductRolePermissions perm = ProductRolePermissions.builder()
                .productId("ALL")
                .role("SUPPORT")
                .permissions(List.of("read"))
                .build();

        ProductRolePermissionsList iamPermissions = ProductRolePermissionsList.builder()
                .items(List.of(perm))
                .build();

        when(iamExternalRestClient._getIAMProductRolePermissionsList(userId, null))
                .thenReturn(ResponseEntity.ok(iamPermissions));

        // when
        Institution result = institutionV2Service.findAllInstitutionById(institutionId);

        // then
        assertNotNull(result);
        assertNotNull(result.getOnboarding());
        assertFalse(result.getOnboarding().isEmpty());

        // Find the product
        var product = result.getOnboarding().stream()
                .filter(p -> "productId".equals(p.getProductId()))
                .findFirst()
                .orElseThrow();

        assertTrue(product.isAuthorized());
        assertEquals("SUPPORT", product.getUserRole());
        assertEquals(List.of("read"), product.getUserProductActions());

        // Checks
        verify(userApiRestClient, never())._getUserInstitutionWithPermission(any(), any(), any());
        verify(coreInstitutionApiRestClient, times(1))._retrieveInstitutionByIdUsingGET(institutionId, null);
        verify(iamExternalRestClient, times(1))._getIAMProductRolePermissionsList(userId, null);
    }

    @Test
    void findInstitutionByIdWithoutInstitutionId() {
        assertThrows(IllegalArgumentException.class, () -> institutionV2Service.findInstitutionById(null));
    }

    @Test
    void findInstitutionByIdNoUserInstitution() {
        String institutionId = "institutionId";
        String userId = "userId";

        ProductGrantedAuthority productGrantedAuthority = new ProductGrantedAuthority(MANAGER, "productRole", "productId");
        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getId()).thenReturn(userId);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SelfCareGrantedAuthority("institutionId", Collections.singleton(productGrantedAuthority))));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        InstitutionResponse institutionResponse = Mockito.mock(InstitutionResponse.class);
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institutionResponse));
        when(userApiRestClient._getUserInstitutionWithPermission(institutionId, userId, null)).thenReturn(ResponseEntity.ok().build());
        assertThrows(AccessDeniedException.class, () -> institutionV2Service.findInstitutionById(institutionId));
        verify(userApiRestClient, Mockito.times(1))._getUserInstitutionWithPermission(institutionId, userId, null);
    }

    @Test
    void findInstitutionByIdNoInstitution() throws IOException {
        String institutionId = "institutionId";
        String userId = "userId";

        ClassPathResource userInstitutionResource = new ClassPathResource("expectations/UserInstitutionWithActions.json");
        byte[] userInstitutionStream = Files.readAllBytes(userInstitutionResource.getFile().toPath());
        UserInstitutionWithActions userInstitutionWithActionsDto = objectMapper.readValue(userInstitutionStream, new TypeReference<>() {
        });

        ProductGrantedAuthority productGrantedAuthority = new ProductGrantedAuthority(MANAGER, "productRole", "productId");
        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getId()).thenReturn(userId);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SelfCareGrantedAuthority("institutionId", Collections.singleton(productGrantedAuthority))));
        SecurityContextHolder.getContext().setAuthentication(authentication);


        when(userApiRestClient._getUserInstitutionWithPermission(institutionId, userId, null)).thenReturn(ResponseEntity.ok(userInstitutionWithActionsDto));
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(null));
        assertThrows(ResourceNotFoundException.class, () -> institutionV2Service.findInstitutionById(institutionId));
    }

    @Test
    void findInstitutionByIdNoOnboarding() throws IOException {
        String institutionId = "institutionId";
        String userId = "userId";

        ClassPathResource userInstitutionResource = new ClassPathResource("expectations/UserInstitutionWithActions.json");
        byte[] userInstitutionStream = Files.readAllBytes(userInstitutionResource.getFile().toPath());
        UserInstitutionWithActions userInstitutionWithActionsDto = objectMapper.readValue(userInstitutionStream, new TypeReference<>() {
        });

        ClassPathResource resource = new ClassPathResource("expectations/Institution.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        InstitutionResponse institution = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });
        institution.setOnboarding(null);

        ProductGrantedAuthority productGrantedAuthority = new ProductGrantedAuthority(MANAGER, "productRole", "productId");
        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getId()).thenReturn(userId);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SelfCareGrantedAuthority("institutionId", Collections.singleton(productGrantedAuthority))));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(userApiRestClient._getUserInstitutionWithPermission(institutionId, userId, null)).thenReturn(ResponseEntity.ok(userInstitutionWithActionsDto));
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));
        assertThrows(ResourceNotFoundException.class, () -> institutionV2Service.findInstitutionById(institutionId));
    }

    @Test
    void findInstitutionByIdLimited() throws IOException {
        String institutionId = "institutionId";
        String userId = "userId";

        ProductGrantedAuthority productGrantedAuthority = new ProductGrantedAuthority(OPERATOR, "productRole", "productId");
        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getId()).thenReturn(userId);
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SelfCareGrantedAuthority("institutionId", Collections.singleton(productGrantedAuthority))));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ClassPathResource userInstitutionResource = new ClassPathResource("expectations/UserInstitutionWithActionsOperator.json");
        byte[] userInstitutionStream = Files.readAllBytes(userInstitutionResource.getFile().toPath());
        UserInstitutionWithActions userInstitutionWithActionsDto = objectMapper.readValue(userInstitutionStream, new TypeReference<>() {
        });

        ClassPathResource resource = new ClassPathResource("stubs/InstitutionResponse.json");
        byte[] resourceStream = Files.readAllBytes(resource.getFile().toPath());
        InstitutionResponse institution = objectMapper.readValue(resourceStream, new TypeReference<>() {
        });

        when(userApiRestClient._getUserInstitutionWithPermission(institutionId, userId, null)).thenReturn(ResponseEntity.ok(userInstitutionWithActionsDto));
        when(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null)).thenReturn(ResponseEntity.ok(institution));

        Institution result = institutionV2Service.findInstitutionById(institutionId);
        Assertions.assertEquals("LIMITED", result.getOnboarding().get(0).getUserRole());
        verify(userApiRestClient, Mockito.times(1))._getUserInstitutionWithPermission(institutionId, userId, null);
    }

    @Test
    void getPendingOnboarding_pendingFound() {
        String productId = "test-product";
        String status = PENDING.name();
        String subunitCode = "test-subunit";
        String taxCode = "test-taxCode";

        OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
        onboardingGetResponse.setItems(List.of(new OnboardingGet().productId(productId)));
        onboardingGetResponse.setCount((long) onboardingGetResponse.getItems().size());

        when(onboardingRestClient._getOnboardingWithFilter(null, null, null, null, productId, null,1, null, status, subunitCode, taxCode, null, null))
                .thenReturn(ResponseEntity.of(Optional.of(onboardingGetResponse)));

        Boolean actualResponse = institutionV2Service.verifyIfExistsPendingOnboarding(taxCode, subunitCode, productId);

        assertTrue(actualResponse);
        verify(onboardingRestClient, Mockito.times(1))
                ._getOnboardingWithFilter(null, null, null, null, productId, null, 1, null, PENDING.name(), subunitCode, taxCode, null, null);
    }

    @Test
    void getOnboardingWithFilterOk() {
        String taxCode = "taxCode";
        String productId = "productId";
        String status = "status";

        OnboardingGet onboardingGet = new OnboardingGet();
        onboardingGet.setProductId(productId);

        OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
        onboardingGetResponse.addItemsItem(onboardingGet);

        doReturn(ResponseEntity.of(Optional.of(onboardingGetResponse)))
                .when(onboardingRestClient)
                ._getOnboardingWithFilter(null, null, null, null, productId, null,1, null, status, null, taxCode, null, null);

        boolean onboardingGetInfo = onboardingRestClient._getOnboardingWithFilter(null, null, null, null, productId, null, 1, null, status, null, taxCode, null, null).getBody() != null;
        Assertions.assertTrue(onboardingGetInfo);
    }

    @Test
    void getPendingOnboarding_pendingNotFound_tobeValidatedFound() {
        OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
        onboardingGetResponse.setItems(List.of(new OnboardingGet().productId("test-product")));
        onboardingGetResponse.setCount((long) onboardingGetResponse.getItems().size());

        when(onboardingRestClient._getOnboardingWithFilter(null, null, null, null, "test-product", null,1, null, PENDING.name(), null, "test-institution", null, null))
                .thenReturn(ResponseEntity.ok().build());
        when(onboardingRestClient._getOnboardingWithFilter(null, null, null, null, "test-product", null, 1, null, TOBEVALIDATED.name(), null, "test-institution", null, null))
                .thenReturn(ResponseEntity.of(Optional.of(onboardingGetResponse)));

        Boolean actualResponse = institutionV2Service.verifyIfExistsPendingOnboarding("test-institution", null, "test-product");

        assertTrue(actualResponse);
        verify(onboardingRestClient, Mockito.times(1))
                ._getOnboardingWithFilter(null, null, null, null, "test-product", null, 1, null, PENDING.name(), null, "test-institution", null, null);
        verify(onboardingRestClient, Mockito.times(1))
                ._getOnboardingWithFilter(null, null, null, null, "test-product", null, 1, null, TOBEVALIDATED.name(), null, "test-institution", null, null);
    }

    @Test
    void getPendingOnboarding_neitherFound() {
        OnboardingGetResponse onboardingGetResponse = new OnboardingGetResponse();
        onboardingGetResponse.setItems(List.of(new OnboardingGet().productId("test-product")));
        onboardingGetResponse.setCount((long) onboardingGetResponse.getItems().size());
        when(onboardingRestClient._getOnboardingWithFilter(null, null, null, null, "test-product", null, 1, null, PENDING.name(), null, "test-institution", null, null))
                .thenReturn(ResponseEntity.ok().build());
        when(onboardingRestClient._getOnboardingWithFilter(null, null, null, null, "test-product", null, 1, null, TOBEVALIDATED.name(), null, "test-institution", null, null))
                .thenReturn(ResponseEntity.ok().build());

        Boolean actualResponse = institutionV2Service.verifyIfExistsPendingOnboarding("test-institution", null, "test-product");

        assertFalse(actualResponse);
    }

    @Test
    void getOnboardingsInfo_WithoutFilter() {
        String institutionId = "institutionId";

        OnboardingResponse onboardingResponsePagopa = new OnboardingResponse();
        onboardingResponsePagopa.setProductId(PROD_PAGOPA.name());
        onboardingResponsePagopa.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        OnboardingResponse onboardingResponseIo = new OnboardingResponse();
        onboardingResponseIo.setProductId(PROD_IO.name());
        onboardingResponseIo.setStatus(OnboardingResponse.StatusEnum.ACTIVE);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(onboardingResponsePagopa, onboardingResponseIo));

        doReturn(ResponseEntity.of(Optional.of(onboardingsResponse)))
                .when(coreInstitutionApiRestClient)
                ._getOnboardingsInstitutionUsingGET(institutionId, null);

        OnboardingsResponse response = institutionV2Service.getOnboardingsInfoResponse(institutionId, null);
        Assertions.assertEquals(2, response.getOnboardings().size());
    }


    @Test
    void getOnboardingsInfo_WithFilter() {
        String institutionId = "institutionId";
        List<String> products = List.of(PROD_PAGOPA.name(), PROD_DASHBOARD_PSP.name());

        OnboardingResponse onboardingResponsePagopa = new OnboardingResponse();
        onboardingResponsePagopa.setProductId(PROD_PAGOPA.name());
        onboardingResponsePagopa.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        OnboardingResponse onboardingResponseDashboard = new OnboardingResponse();
        onboardingResponseDashboard.setProductId(PROD_DASHBOARD_PSP.name());
        onboardingResponseDashboard.setStatus(OnboardingResponse.StatusEnum.DELETED);
        OnboardingResponse onboardingResponseIo = new OnboardingResponse();
        onboardingResponseIo.setProductId(PROD_IO.name());
        onboardingResponseIo.setStatus(OnboardingResponse.StatusEnum.ACTIVE);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(onboardingResponsePagopa, onboardingResponseDashboard, onboardingResponseIo));

        doReturn(ResponseEntity.of(Optional.of(onboardingsResponse)))
                .when(coreInstitutionApiRestClient)
                ._getOnboardingsInstitutionUsingGET(institutionId, null);

        OnboardingsResponse response = institutionV2Service.getOnboardingsInfoResponse(institutionId, products);
        Assertions.assertEquals(1, response.getOnboardings().size());
    }

    @Test
    void getContract_shouldReturnContract_whenActiveOnboardingExists() {
        String institutionId = "inst1";
        String productId = "prod1";
        String tokenId = "token123";
        Resource expectedContract = new ByteArrayResource("contract".getBytes());

        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        onboarding.setProductId(productId);
        onboarding.setCreatedAt(OffsetDateTime.now());
        onboarding.setTokenId(tokenId);
        onboarding.setInstitutionType(OnboardingResponse.InstitutionTypeEnum.PSP);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(onboarding));

        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(onboardingsResponse));
        when(documentContentRestClient._getContractSigned(tokenId))
                .thenReturn(ResponseEntity.ok(expectedContract));

        Resource result = institutionV2Service.getContract(institutionId, productId);

        assertEquals(expectedContract, result);
    }

    @Test
    void getContract_shouldThrowException_whenActiveOnboardingExists_noPSP() {
        String institutionId = "inst1";
        String productId = "prod1";
        String tokenId = "token123";

        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        onboarding.setCreatedAt(OffsetDateTime.now());
        onboarding.setProductId(productId);
        onboarding.setTokenId(tokenId);
        onboarding.setInstitutionType(OnboardingResponse.InstitutionTypeEnum.GSP);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(onboarding));

        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(onboardingsResponse));

        assertThrows(AccessDeniedException.class, () ->
                institutionV2Service.getContract(institutionId, productId)
        );
    }

    @Test
    void getContract_shouldThrowException_whenActiveOnboardingExists_noPSP_issuerPAGOPA() {
        String institutionId = "inst1";
        String productId = "prod1";
        String tokenId = "token123";
        Resource expectedContract = new ByteArrayResource("contract".getBytes());

        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        onboarding.setCreatedAt(OffsetDateTime.now());
        onboarding.setProductId(productId);
        onboarding.setTokenId(tokenId);
        onboarding.setInstitutionType(OnboardingResponse.InstitutionTypeEnum.GSP);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(onboarding));

        // Mock SelfCareUser with issuer = PAGOPA
        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getIssuer()).thenReturn("PAGOPA");

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(onboardingsResponse));
        when(documentContentRestClient._getContractSigned(tokenId))
                .thenReturn(ResponseEntity.ok(expectedContract));

        Resource result = institutionV2Service.getContract(institutionId, productId);

        assertEquals(expectedContract, result);
    }

    @Test
    void getContract_shouldThrowException_whenNoActiveOnboarding() {
        String institutionId = "inst1";
        String productId = "prod1";

        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setStatus(OnboardingResponse.StatusEnum.DELETED);
        onboarding.setProductId(productId);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(onboarding));

        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(onboardingsResponse));

        assertThrows(ResourceNotFoundException.class, () ->
                institutionV2Service.getContract(institutionId, productId)
        );
    }

    @Test
    void getContract_shouldHandleNullResponseBody() {
        String institutionId = "inst1";
        String productId = "prod1";

        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(null));

        assertThrows(ResourceNotFoundException.class, () ->
                institutionV2Service.getContract(institutionId, productId)
        );
    }

    @Test
    void getContract_shouldReturnMostRecentActiveOnboarding() {
        String institutionId = "inst1";
        String productId = "prod1";
        String tokenId = "token-latest";
        Resource expectedContract = new ByteArrayResource("contract".getBytes());

        OnboardingResponse older = new OnboardingResponse();
        older.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        older.setCreatedAt(OffsetDateTime.now().minusDays(1));
        older.setTokenId("token-old");
        older.setProductId(productId);

        OnboardingResponse newer = new OnboardingResponse();
        newer.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        newer.setCreatedAt(OffsetDateTime.now());
        newer.setTokenId(tokenId);
        newer.setProductId(productId);
        newer.setInstitutionType(OnboardingResponse.InstitutionTypeEnum.PSP);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(older, newer));

        SelfCareUser principal = Mockito.mock(SelfCareUser.class);
        when(principal.getIssuer()).thenReturn("PAGOPA");

        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(onboardingsResponse));
        when(documentContentRestClient._getContractSigned(tokenId))
                .thenReturn(ResponseEntity.ok(expectedContract));

        Resource result = institutionV2Service.getContract(institutionId, productId);

        assertEquals(expectedContract, result);
    }

    @Test
    void checkAttachmentStatus_true() {
        String institutionId = "inst1";
        String productId = "prod1";
        String name = "name";
        String tokenId = "token123";

        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        onboarding.setProductId(productId);
        onboarding.setCreatedAt(OffsetDateTime.now());
        onboarding.setTokenId(tokenId);
        onboarding.setInstitutionType(OnboardingResponse.InstitutionTypeEnum.PSP);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(onboarding));

        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(onboardingsResponse));
        when(documentRestClient._headAttachment(tokenId, name))
                .thenReturn(ResponseEntity.noContent().build());

        Boolean result = institutionV2Service.checkAttachmentStatus(institutionId, productId, name);

        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void checkAttachmentStatus_false() {
        String institutionId = "inst1";
        String productId = "prod1";
        String name = "name";
        String tokenId = "token123";

        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        onboarding.setProductId(productId);
        onboarding.setCreatedAt(OffsetDateTime.now());
        onboarding.setTokenId(tokenId);
        onboarding.setInstitutionType(OnboardingResponse.InstitutionTypeEnum.PSP);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(onboarding));

        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(onboardingsResponse));
        when(documentRestClient._headAttachment(tokenId, name))
                .thenReturn(ResponseEntity.notFound().build());

        Boolean result = institutionV2Service.checkAttachmentStatus(institutionId, productId, name);

        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void checkAttachmentStatus_exception() {
        String institutionId = "inst1";
        String productId = "prod1";
        String name = "name";
        String tokenId = "token123";

        OnboardingResponse onboarding = new OnboardingResponse();
        onboarding.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        onboarding.setProductId(productId);
        onboarding.setCreatedAt(OffsetDateTime.now());
        onboarding.setTokenId(tokenId);
        onboarding.setInstitutionType(OnboardingResponse.InstitutionTypeEnum.PSP);

        OnboardingsResponse onboardingsResponse = new OnboardingsResponse();
        onboardingsResponse.setOnboardings(List.of(onboarding));

        when(coreInstitutionApiRestClient._getOnboardingsInstitutionUsingGET(institutionId, null))
                .thenReturn(ResponseEntity.ok(onboardingsResponse));

        when(documentRestClient._headAttachment(tokenId, name))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        Boolean result = institutionV2Service.checkAttachmentStatus(institutionId, productId, name);

        assertEquals(Boolean.FALSE, result);
    }
}
