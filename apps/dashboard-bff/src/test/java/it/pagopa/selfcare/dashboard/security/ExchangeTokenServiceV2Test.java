package it.pagopa.selfcare.dashboard.security;

import io.jsonwebtoken.Jwts;
import it.pagopa.selfcare.commons.base.security.ProductGrantedAuthority;
import it.pagopa.selfcare.commons.base.security.SelfCareGrantedAuthority;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.commons.web.security.JwtService;
import it.pagopa.selfcare.dashboard.client.IamExternalRestClient;
import it.pagopa.selfcare.dashboard.client.UserInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.config.ExchangeTokenProperties;
import it.pagopa.selfcare.dashboard.model.ExchangedToken;
import it.pagopa.selfcare.dashboard.model.groups.UserGroup;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionResourceMapperImpl;
import it.pagopa.selfcare.dashboard.model.product.mapper.ProductMapper;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.service.InstitutionService;
import it.pagopa.selfcare.dashboard.service.UserGroupV2Service;
import it.pagopa.selfcare.dashboard.service.UserV2Service;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissions;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissionsList;
import it.pagopa.selfcare.product.entity.Product;
import it.pagopa.selfcare.product.service.ProductService;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductState;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

import static it.pagopa.selfcare.commons.base.security.PartyRole.MANAGER;
import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeTokenServiceV2Test {

    @Mock
    private UserInstitutionApiRestClient userInstitutionApiRestClient;

    @Mock
    private IamExternalRestClient iamExternalRestClient;

    @Mock
    private InstitutionService institutionService;

    @Mock
    private ProductService productService;

    @Spy
    private InstitutionResourceMapperImpl institutionResourceMapper;

    @Spy
    private InstitutionMapperImpl institutionMapper;

    @Spy
    private ProductMapper productMapper;

    @Mock
    private UserGroupV2Service userGroupV2Service;

    @Mock
    private ExchangeTokenProperties exchangeTokenProperties;


    @Mock
    private UserV2Service userV2Service;

    @Mock
    private JwtService jwtService;

    private ExchangeTokenServiceV2 exchangeTokenServiceV2;

    @BeforeEach
    void setUp() throws Exception {
        when(exchangeTokenProperties.getBillingAudience()).thenReturn("aud");
        when(exchangeTokenProperties.getBillingUrl()).thenReturn("url");
        when(exchangeTokenProperties.getDuration()).thenReturn("PT20H30M");

        when(exchangeTokenProperties.getKid()).thenReturn("kid");
        when(exchangeTokenProperties.getIssuer()).thenReturn("issuer");
        File file = ResourceUtils.getFile("classpath:certs/PKCS8key.pem");
        String jwtSigningKey = Files.readString(file.toPath(), Charset.defaultCharset());
        when(exchangeTokenProperties.getSigningKey()).thenReturn(jwtSigningKey);

        exchangeTokenServiceV2 = new ExchangeTokenServiceV2(jwtService,
                institutionService,userGroupV2Service,exchangeTokenProperties,userV2Service,productService,
                userInstitutionApiRestClient,iamExternalRestClient,institutionResourceMapper,institutionMapper, productMapper);
    }


    @Test
    void exchange_validInputs_returnsExchangedToken() {
        String jti = "id";
        String sub = "subject";
        Date iat = Date.from(Instant.now().minusSeconds(1));
        Date exp = Date.from(iat.toInstant().plusSeconds(5));
        String institutionId = "institutionId";
        String productId = "productId";
        String credential = "password";
        String userId = UUID.randomUUID().toString();
        UserInstitutionResponse userInstitution = mockInstance(new UserInstitutionResponse());
        OnboardedProductResponse onboardedProductResponse = mockInstance(new OnboardedProductResponse());
        onboardedProductResponse.setProductId(productId);
        onboardedProductResponse.setRole("MANAGER");
        onboardedProductResponse.setStatus(OnboardedProductState.ACTIVE);
        userInstitution.setProducts(List.of(onboardedProductResponse));
        it.pagopa.selfcare.dashboard.model.institution.Institution institution = mock(it.pagopa.selfcare.dashboard.model.institution.Institution.class);
        Product product = mock(Product.class);

        List<ProductGrantedAuthority> roleOnProducts = List.of(new ProductGrantedAuthority(MANAGER, "productRole", productId));
        List<GrantedAuthority> authorities = List.of(new SelfCareGrantedAuthority("differentInstitutionId", roleOnProducts));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(SelfCareUser.builder(userId).build(), "password", authorities);
        TestSecurityContextHolder.setAuthentication(authentication);

        TestSecurityContextHolder.setAuthentication(authentication);
        when(jwtService.getClaims(credential))
                .thenReturn(Jwts.claims()
                        .setId(jti)
                        .setSubject(sub)
                        .setIssuedAt(iat)
                        .setExpiration(exp));

        when(userInstitutionApiRestClient._retrieveUserInstitutions(any(), any(), any(), any(), any(), any())).thenReturn(ResponseEntity.ok(List.of(userInstitution)));
        when(institutionService.getInstitutionById(institutionId)).thenReturn(institution);
        when(productService.getProduct(productId)).thenReturn(product);
        Page<UserGroup> userGroups = Page.empty();
        when(userGroupV2Service.getUserGroups(eq(institutionId), eq(productId), any(),
                eq(Pageable.ofSize(100)))).thenReturn(userGroups);
        when(userV2Service.getUserById(userId, null, null)).thenReturn(mockInstance(new User()));

        ExchangedToken result = exchangeTokenServiceV2.exchange(institutionId, productId, Optional.empty());

        assertNotNull(result);
    }

    @Test
    void exchange_noAuth() {
        // Arrange
        String institutionId = "validInstitutionId";
        String productId = "validProductId";
        Optional<String> environment = Optional.of("validEnvironment");
        SecurityContextHolder.getContext().setAuthentication(null);

        Assertions.assertThrows(IllegalStateException.class, () -> exchangeTokenServiceV2.exchange(institutionId, productId, environment), "Authentication is required");
    }

    @Test
    void exchange_noProductGrantedAuthority_throwsIllegalArgumentException() {
        String institutionId = "institutionId";
        String productId = "productId";
        String userId = UUID.randomUUID().toString();
        UserInstitutionResponse userInstitution = mockInstance(new UserInstitutionResponse());
        userInstitution.setProducts(Collections.emptyList());

        List<ProductGrantedAuthority> roleOnProducts = List.of(new ProductGrantedAuthority(MANAGER, "productRole", "invalid"));
        List<GrantedAuthority> authorities = List.of(new SelfCareGrantedAuthority("differentInstitutionId", roleOnProducts));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(SelfCareUser.builder(userId).build(), "password", authorities);
        TestSecurityContextHolder.setAuthentication(authentication);

        TestSecurityContextHolder.setAuthentication(authentication);

        when(userInstitutionApiRestClient._retrieveUserInstitutions(any(), any(), any(), any(), any(), any())).thenReturn(ResponseEntity.ok(List.of(userInstitution)));

        final Optional<String> environment = Optional.empty();
        assertThrows(IllegalArgumentException.class, () -> exchangeTokenServiceV2.exchange(institutionId, productId, environment));
    }

    @Test
    void exchangeBackofficeAdmin_validInputs_returnsExchangedToken() {
        String jti = "id";
        String sub = "subject";
        String iss = "PAGOPA";
        Date iat = Date.from(Instant.now().minusSeconds(1));
        Date exp = Date.from(iat.toInstant().plusSeconds(5));
        String institutionId = "institutionId";
        String productId = "productId";
        String credential = "password";
        String userId = UUID.randomUUID().toString();

        ProductRolePermissionsList permissions = new ProductRolePermissionsList();
        permissions.setItems(List.of(
                new ProductRolePermissions().productId("productId").role("SUPPORT").group("support").permissions(List.of("p1", "p2")),
                new ProductRolePermissions().productId("productId").role("LEGAL").group("support").permissions(List.of("p1")),
                new ProductRolePermissions().productId("productId").role("ADMIN").group("support").permissions(List.of("p1", "p2", "p3"))
        ));

        it.pagopa.selfcare.dashboard.model.institution.Institution institution = mock(it.pagopa.selfcare.dashboard.model.institution.Institution.class);
        Product product = mock(Product.class);

        TestSecurityContextHolder.setAuthentication(new TestingAuthenticationToken(SelfCareUser.builder(userId).build(), "password"));
        when(jwtService.getClaims(credential))
                .thenReturn(Jwts.claims()
                        .setId(jti)
                        .setIssuer(iss)
                        .setSubject(sub)
                        .setIssuedAt(iat)
                        .setExpiration(exp));

        when(institutionService.getInstitutionById(institutionId)).thenReturn(institution);
        when(productService.getProduct(productId)).thenReturn(product);
        when(iamExternalRestClient._getIAMProductRolePermissionsList(userId, productId)).thenReturn(ResponseEntity.ok(permissions));

        ExchangedToken result = exchangeTokenServiceV2.exchangeBackofficeAdmin(institutionId, productId, Optional.empty());

        assertNotNull(result);
        assertNotNull(result.getIdentityToken());
    }

    @Test
    void exchangeBackofficeAdmin_noAuth() {
        // Arrange
        String institutionId = "validInstitutionId";
        String productId = "validProductId";
        Optional<String> environment = Optional.of("validEnvironment");
        SecurityContextHolder.getContext().setAuthentication(null);

        Assertions.assertThrows(IllegalStateException.class, () -> exchangeTokenServiceV2.exchangeBackofficeAdmin(institutionId, productId, environment), "Authentication is required");
    }

    @Test
    void exchangeBackofficeAdmin_noUserClaims_throwsIllegalArgumentException() {
        String institutionId = "institutionId";
        String productId = "productId";
        String credential = "password";
        String userId = UUID.randomUUID().toString();

        it.pagopa.selfcare.dashboard.model.institution.Institution institution = mock(it.pagopa.selfcare.dashboard.model.institution.Institution.class);

        TestSecurityContextHolder.setAuthentication(new TestingAuthenticationToken(SelfCareUser.builder(userId).build(), credential));

        when(institutionService.getInstitutionById(institutionId)).thenReturn(institution);
        when(iamExternalRestClient._getIAMProductRolePermissionsList(userId, productId))
                .thenReturn(ResponseEntity.notFound().build());

        final Optional<String> environment = Optional.empty();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exchangeTokenServiceV2.exchangeBackofficeAdmin(
                        institutionId,
                        productId,
                        environment
                )
        );

        Assertions.assertEquals("Session token claims is required", exception.getMessage()
        );
    }

    @Test
    void testRetrieveBillingExchangedToken_AuthenticationMissing() {
        // Simulate authentication being null
        SecurityContextHolder.getContext().setAuthentication(null);

        // Expect IllegalStateException
        assertThrows(IllegalStateException.class, () -> exchangeTokenServiceV2.retrieveBillingExchangedToken("institutionId"));
    }

    @Test
    void testRetrieveBillingExchangedToken_ValidAuthentication() {
        String jti = "id";
        String sub = "subject";
        Date iat = Date.from(Instant.now().minusSeconds(1));
        Date exp = Date.from(iat.toInstant().plusSeconds(5));
        String institutionId = "institutionId";
        String productId = "productId";
        String credential = "password";
        String userId = UUID.randomUUID().toString();


        List<ProductGrantedAuthority> roleOnProducts = List.of(new ProductGrantedAuthority(MANAGER, "productRole", productId));
        List<GrantedAuthority> authorities = List.of(new SelfCareGrantedAuthority("differentInstitutionId", roleOnProducts));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(SelfCareUser.builder(userId).build(), "password", authorities);
        TestSecurityContextHolder.setAuthentication(authentication);

        TestSecurityContextHolder.setAuthentication(authentication);

        UserInstitutionResponse userInstitution = mockInstance(new UserInstitutionResponse());
        OnboardedProductResponse onboardedProductResponse = mockInstance(new OnboardedProductResponse());
        onboardedProductResponse.setProductId(productId);
        onboardedProductResponse.setRole("MANAGER");
        onboardedProductResponse.setStatus(OnboardedProductState.ACTIVE);
        userInstitution.setProducts(List.of(onboardedProductResponse));

        it.pagopa.selfcare.dashboard.model.institution.Institution institution = mockInstance(new it.pagopa.selfcare.dashboard.model.institution.Institution());
        when(jwtService.getClaims(credential))
                .thenReturn(Jwts.claims()
                        .setId(jti)
                        .setSubject(sub)
                        .setIssuedAt(iat)
                        .setExpiration(exp));

        when(userInstitutionApiRestClient._retrieveUserInstitutions(
                institutionId,
                null,
                null,
                null,
                null,
                userId
        )).thenReturn(ResponseEntity.ok(List.of(userInstitution)));

        Page<UserGroup> userGroups = Page.empty();
        when(userGroupV2Service.getUserGroups(institutionId, null, UUID.fromString(userId),
                Pageable.ofSize(100))).thenReturn(userGroups);

        when(institutionService.getInstitutionById(institutionId)).thenReturn(institution);
        when(productService.getProducts(false, true)).thenReturn(new ArrayList<>()); // Mock an empty product list
        when(userV2Service.getUserById(userId, null, null)).thenReturn(mockInstance(new User()));

        // When
        ExchangedToken exchangedToken = exchangeTokenServiceV2.retrieveBillingExchangedToken(institutionId);

        // Then
        assertNotNull(exchangedToken);
    }

    @Test
    void testRetrieveBillingExchangedToken_InvalidInstitution() {
        String institutionId = "institutionId";
        String productId = "productId";
        String userId = UUID.randomUUID().toString();


        List<ProductGrantedAuthority> roleOnProducts = List.of(new ProductGrantedAuthority(MANAGER, "productRole", productId));
        List<GrantedAuthority> authorities = List.of(new SelfCareGrantedAuthority("differentInstitutionId", roleOnProducts));
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(SelfCareUser.builder(userId).build(), "password", authorities);
        TestSecurityContextHolder.setAuthentication(authentication);

        TestSecurityContextHolder.setAuthentication(authentication);

        UserInstitutionResponse userInstitution = mockInstance(new UserInstitutionResponse());
        OnboardedProductResponse onboardedProductResponse = mockInstance(new OnboardedProductResponse());
        onboardedProductResponse.setProductId(productId);
        onboardedProductResponse.setRole("MANAGER");
        onboardedProductResponse.setStatus(OnboardedProductState.ACTIVE);
        userInstitution.setProducts(List.of(onboardedProductResponse));

        when(userInstitutionApiRestClient._retrieveUserInstitutions(
                institutionId,
                null,
                null,
                null,
                null,
                userId
        )).thenReturn(ResponseEntity.ok(List.of(userInstitution)));

        when(institutionService.getInstitutionById(institutionId)).thenReturn(null);
        when(productService.getProducts(false, true)).thenReturn(new ArrayList<>());

        assertThrows(IllegalArgumentException.class, () -> exchangeTokenServiceV2.retrieveBillingExchangedToken(institutionId));
    }

}