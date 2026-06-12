package it.pagopa.selfcare.dashboard.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingsResponse;
import it.pagopa.selfcare.dashboard.model.CreateUserDto;
import it.pagopa.selfcare.dashboard.model.InstitutionBaseResource;
import it.pagopa.selfcare.dashboard.model.InstitutionUserDetailsResource;
import it.pagopa.selfcare.dashboard.model.SearchUserDto;
import it.pagopa.selfcare.dashboard.model.delegation.*;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.institution.InstitutionBase;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionResourceMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.OnboardingMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperImpl;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperV2Impl;
import it.pagopa.selfcare.dashboard.model.user.*;
import it.pagopa.selfcare.dashboard.service.DelegationService;
import it.pagopa.selfcare.dashboard.service.InstitutionV2Service;
import it.pagopa.selfcare.dashboard.service.UserV2Service;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.*;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static it.pagopa.selfcare.commons.utils.TestUtils.mockInstance;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO_PREMIUM;
import static it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductState.PENDING;
import static it.pagopa.selfcare.user.generated.openapi.v1.dto.PartyRole.DELEGATE;
import static it.pagopa.selfcare.user.generated.openapi.v1.dto.PartyRole.MANAGER;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InstitutionV2ControllerTest extends BaseControllerTest {

    private static final String BASE_URL = "/v2/institutions";
    private static final String FILE_JSON_PATH = "src/test/resources/json/";

    @InjectMocks
    private InstitutionV2Controller institutionV2Controller;
    @Mock
    private UserV2Service userServiceMock;
    @Mock
    private InstitutionV2Service institutionV2ServiceMock;
    @Mock
    DelegationService delegationServiceMock;
    @Spy
    private UserMapperV2Impl userMapper;
    @Spy
    private OnboardingMapperImpl onboardingMapper;
    @Spy
    private UserMapperImpl userMapperImpl;
    @Spy
    private InstitutionResourceMapperImpl institutionResourceMapper;

    @BeforeEach
    void setUp() {
        super.setUp(institutionV2Controller);
    }

    /**
     * Method under test: {@link InstitutionV2Controller#getInstitutionUser(String, String, Authentication)}
     */
    @Test
    void getInstitutionUser_notNullUser() throws Exception {
        //given
        final String institutionId = "institutionId";
        final String userId = "userId";

        ClassPathResource resource = new ClassPathResource("json/UserInfo.json");
        byte[] userInfoStream = Files.readAllBytes(resource.getFile().toPath());
        UserInfo userInfo = objectMapper.readValue(userInfoStream, UserInfo.class);

        String loggedUserId = "loggedUserId";
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(loggedUserId).build());

        when(institutionV2ServiceMock.getInstitutionUser(institutionId, userId, loggedUserId))
                .thenReturn(userInfo);

        ClassPathResource pathResourceInstitution = new ClassPathResource("json/InstitutionUserDetailsResource.json");
        byte[] resourceStreamInstitution = Files.readAllBytes(pathResourceInstitution.getFile().toPath());
        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/users/{userId}", institutionId, userId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(new String(resourceStreamInstitution)))
                .andReturn();
        //then
        verify(institutionV2ServiceMock, times(1))
                .getInstitutionUser(institutionId, userId, loggedUserId);
        verifyNoMoreInteractions(institutionV2ServiceMock);
    }

    @Test
    void getAllInstitutionUser() throws Exception {
        final String institutionId = "institutionId";
        final String userId = UUID.randomUUID().toString();
        final String loggedUserId = "loggedUserId";
        final Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(loggedUserId).build());

        final UserProductResponse userProductResponse = buildUserProductResponse(userId);
        when(institutionV2ServiceMock.getAllInstitutionUser(institutionId, userId, loggedUserId)).thenReturn(userProductResponse);

        final var response = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/all/{institutionId}/users/{userId}", institutionId, userId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        final InstitutionUserDetailsResource actualResponse = objectMapper.readValue(response.getResponse().getContentAsString(), InstitutionUserDetailsResource.class);
        assertEquals(userId, actualResponse.getId().toString());
        assertEquals("name", actualResponse.getName());
        assertEquals("surname", actualResponse.getSurname());
        assertEquals("test@test.com", actualResponse.getEmail());
        assertEquals("ABC123", actualResponse.getFiscalCode());
        assertEquals("1234567890", actualResponse.getMobilePhone());
        assertNull(actualResponse.getStatus());
        assertNull(actualResponse.getRole());
        assertEquals(2, actualResponse.getProducts().size());

        // prod-y
        assertEquals("prod-y", actualResponse.getProducts().get(0).getId());
        // prod-y role 0
        assertEquals("admin", actualResponse.getProducts().get(0).getRoleInfos().get(0).getRole());
        assertEquals("DELEGATE", actualResponse.getProducts().get(0).getRoleInfos().get(0).getPartyRole());
        assertEquals("ACTIVE", actualResponse.getProducts().get(0).getRoleInfos().get(0).getStatus());
        // prod-y role 1
        assertEquals("operator", actualResponse.getProducts().get(0).getRoleInfos().get(1).getRole());
        assertEquals("OPERATOR", actualResponse.getProducts().get(0).getRoleInfos().get(1).getPartyRole());
        assertEquals("DELETED", actualResponse.getProducts().get(0).getRoleInfos().get(1).getStatus());

        // prod-x
        assertEquals("prod-x", actualResponse.getProducts().get(1).getId());
        // prod-x role 0
        assertEquals("operator3", actualResponse.getProducts().get(1).getRoleInfos().get(0).getRole());
        assertEquals("OPERATOR", actualResponse.getProducts().get(1).getRoleInfos().get(0).getPartyRole());
        assertEquals("ACTIVE", actualResponse.getProducts().get(1).getRoleInfos().get(0).getStatus());
        // prod-x role 1
        assertEquals("operator1", actualResponse.getProducts().get(1).getRoleInfos().get(1).getRole());
        assertEquals("OPERATOR", actualResponse.getProducts().get(1).getRoleInfos().get(1).getPartyRole());
        assertEquals("DELETED", actualResponse.getProducts().get(1).getRoleInfos().get(1).getStatus());
        // prod-x role 2
        assertEquals("operator2", actualResponse.getProducts().get(1).getRoleInfos().get(2).getRole());
        assertEquals("OPERATOR", actualResponse.getProducts().get(1).getRoleInfos().get(2).getPartyRole());
        assertEquals("SUSPENDED", actualResponse.getProducts().get(1).getRoleInfos().get(2).getStatus());
    }

    private UserProductResponse buildUserProductResponse(String userId) {
        final UserProductResponse userProductResponse = new UserProductResponse();
        userProductResponse.setId(userId);
        userProductResponse.setEmail("test@test.com");
        userProductResponse.setName("name");
        userProductResponse.setSurname("surname");
        userProductResponse.setMobilePhone("1234567890");
        userProductResponse.setTelephone("0987654321");
        userProductResponse.setTaxCode("ABC123");
        userProductResponse.setProducts(List.of(
                buildOnboardedProductResponse("prod-x", "OPERATOR", "operator3", OnboardedProductState.ACTIVE),
                buildOnboardedProductResponse("prod-x", "OPERATOR", "operator1", OnboardedProductState.DELETED),
                buildOnboardedProductResponse("prod-x", "OPERATOR", "operator2", OnboardedProductState.SUSPENDED),
                buildOnboardedProductResponse("prod-y", "DELEGATE", "admin", OnboardedProductState.ACTIVE),
                buildOnboardedProductResponse("prod-y", "OPERATOR", "operator", OnboardedProductState.DELETED)
        ));
        return userProductResponse;
    }

    private OnboardedProductResponse buildOnboardedProductResponse(String productId, String role, String productRole, OnboardedProductState status) {
        final OnboardedProductResponse onboardedProductResponse = new OnboardedProductResponse();
        onboardedProductResponse.setRoleId(UUID.randomUUID().toString());
        onboardedProductResponse.setTokenId(UUID.randomUUID().toString());
        onboardedProductResponse.setProductId(productId);
        onboardedProductResponse.setEnv(Env.ROOT);
        onboardedProductResponse.setRole(role);
        onboardedProductResponse.setProductRole(productRole);
        onboardedProductResponse.setStatus(status);
        onboardedProductResponse.setCreatedAt(LocalDateTime.now());
        onboardedProductResponse.setUpdatedAt(LocalDateTime.now());
        return onboardedProductResponse;
    }

    /**
     * Method under test: {@link InstitutionV2Controller#getInstitutions(Authentication)}
     */
    @Test
    void getInstitutions_institutionInfoNotNull() throws Exception {
        // given
        final String userId = "userId";
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(SelfCareUser.builder(userId).build());

        InstitutionBase expectedInstitution = getInstitutionBase();
        List<InstitutionBase> expectedInstitutionInfos = new ArrayList<>();
        expectedInstitutionInfos.add(expectedInstitution);
        when(userServiceMock.getInstitutions(userId)).thenReturn(expectedInstitutionInfos);
        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        // then
        List<InstitutionBaseResource> resources = objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        assertNotNull(resources);
        assertEquals(resources.get(0).getId(), expectedInstitution.getId());
        assertEquals("ADMIN", resources.get(0).getUserRole());
        assertEquals(resources.get(0).getName(), expectedInstitution.getName());
        assertEquals(resources.get(0).getStatus(), expectedInstitution.getStatus());
        assertEquals(resources.get(0).getParentDescription(), expectedInstitution.getParentDescription());

        verify(userServiceMock, times(1))
                .getInstitutions(userId);
        verifyNoMoreInteractions(userServiceMock);
    }

    private InstitutionBase getInstitutionBase() {
        InstitutionBase expectedInstitution = mockInstance(new InstitutionBase());
        expectedInstitution.setId("123456");
        expectedInstitution.setUserRole("MANAGER");
        expectedInstitution.setName("name");
        expectedInstitution.setStatus("status");
        expectedInstitution.setParentDescription("parentDescription");
        return expectedInstitution;
    }

    @Test
    void getInstitution_institutionInfoNotNull() throws Exception {
        String institutionId = "institutionId";
        byte[] institutionStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "Institution.json"));
        Institution institution = objectMapper.readValue(institutionStream, Institution.class);

        when(institutionV2ServiceMock.findInstitutionById(institutionId)).thenReturn(institution);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}", institutionId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(new String(Files.readAllBytes(Paths.get(FILE_JSON_PATH + "InstitutionResource.json")))))
                .andReturn();
    }

    @Test
    void getAllInstitution_institutionInfoNotNull() throws Exception {
        String institutionId = "institutionId";
        byte[] institutionStream = Files.readAllBytes(Paths.get(FILE_JSON_PATH + "Institution.json"));
        Institution institution = objectMapper.readValue(institutionStream, Institution.class);

        when(institutionV2ServiceMock.findAllInstitutionById(institutionId)).thenReturn(institution);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/all/{institutionId}", institutionId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().json(new String(Files.readAllBytes(Paths.get(FILE_JSON_PATH + "InstitutionResource.json")))))
                .andReturn();
    }

    @Test
    void addUserProductRolesWithoutPartyRole_happyPath() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";
        UserProductRoles userProductRoles = new UserProductRoles();
        userProductRoles.setProductRoles(Set.of("admin"));
        userProductRoles.setToAddOnAggregates(true);

        Authentication authentication = mock(Authentication.class);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/{institutionId}/products/{productId}/users/{userId}", institutionId, productId, userId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(userProductRoles))
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andReturn();

        // then
        verify(userServiceMock, times(1))
                .addUserProductRoles(institutionId, productId, userId, userProductRoles.getToAddOnAggregates(), userProductRoles.getProductRoles(), userProductRoles.getRole());
        verifyNoMoreInteractions(userServiceMock);
    }


    @Test
    void addUserProductRolesWithPartyRole_happyPath() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";
        UserProductRoles userProductRoles = new UserProductRoles();
        userProductRoles.setProductRoles(Set.of("admin"));
        userProductRoles.setRole("MANAGER");
        userProductRoles.setToAddOnAggregates(true);

        Authentication authentication = mock(Authentication.class);

        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/{institutionId}/products/{productId}/users/{userId}", institutionId, productId, userId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(userProductRoles))
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andReturn();

        // then
        verify(userServiceMock, times(1))
                .addUserProductRoles(institutionId, productId, userId, userProductRoles.getToAddOnAggregates(), userProductRoles.getProductRoles(), userProductRoles.getRole());
        verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void addUserProductRoles_whenUserProductRolesIsNull() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String userId = "userId";

        Authentication authentication = mock(Authentication.class);

        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .put(BASE_URL + "/{institutionId}/products/{productId}/users/{userId}", institutionId, productId, userId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(null))
                        .accept(APPLICATION_JSON_VALUE))
                .andReturn();

        // then
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        verifyNoInteractions(userServiceMock);
    }

    @Test
    void createInstitutionProductUserWithoutRole_happyPath() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";
        CreateUserDto createUserDto = new CreateUserDto();
        createUserDto.setName("John");
        createUserDto.setSurname("Doe");
        createUserDto.setTaxCode("ABC123");
        createUserDto.setEmail("john.doe@example.com");
        createUserDto.setProductRoles(Set.of("admin"));
        createUserDto.setToAddOnAggregates(true);

        UserToCreate userToCreate = userMapper.toUserToCreate(createUserDto);

        String id = UUID.randomUUID().toString();

        Authentication authentication = mock(Authentication.class);
        when(userServiceMock.createUsers(institutionId, productId, userToCreate)).thenReturn(id);
        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/{institutionId}/products/{productId}/users", institutionId, productId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(createUserDto))
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(id)))
                .andReturn();

        // then
        verify(userServiceMock, times(1))
                .createUsers(institutionId, productId, userToCreate);
        verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void createInstitutionProductUser_happyPath() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";
        CreateUserDto createUserDto = new CreateUserDto();
        createUserDto.setName("John");
        createUserDto.setSurname("Doe");
        createUserDto.setTaxCode("ABC123");
        createUserDto.setEmail("john.doe@example.com");
        createUserDto.setProductRoles(Set.of("admin"));
        createUserDto.setRole("MANAGER");
        createUserDto.setToAddOnAggregates(true);

        UserToCreate userToCreate = userMapper.toUserToCreate(createUserDto);

        String id = UUID.randomUUID().toString();

        Authentication authentication = mock(Authentication.class);
        when(userServiceMock.createUsers(institutionId, productId, userToCreate)).thenReturn(id);
        // when
        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/{institutionId}/products/{productId}/users", institutionId, productId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(createUserDto))
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(id)))
                .andReturn();

        // then
        verify(userServiceMock, times(1))
                .createUsers(institutionId, productId, userToCreate);
        verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void createInstitutionProductUser_whenCreateUserDtoIsNull() throws Exception {
        // given
        final String institutionId = "institutionId";
        final String productId = "productId";

        Authentication authentication = mock(Authentication.class);
        // when
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/{institutionId}/products/{productId}/users", institutionId, productId)
                        .principal(authentication)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(null))
                        .accept(APPLICATION_JSON_VALUE))
                .andReturn();

        // then
        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getResponse().getStatus());
        verifyNoInteractions(userServiceMock);
    }

    @Test
    void getDelegationsUsingTo_shouldGetDataWithoutFilters() throws Exception {
        // Given
        DelegationWithInfo expectedDelegation = dummyDelegation();
        PageInfo exptectedPageInfo = new PageInfo(10000, 0, 1, 1);

        GetDelegationParameters delegationParameters = GetDelegationParameters.builder()
                .to("to")
                .build();

        DelegationWithPagination expectedDelegationWithPagination = new DelegationWithPagination(List.of(expectedDelegation), exptectedPageInfo);

        when(delegationServiceMock.getDelegationsV2(delegationParameters)).thenReturn(expectedDelegationWithPagination);
        // When

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/institutions", expectedDelegation.getBrokerId()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andReturn();

        DelegationWithPagination resource = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        // Then
        AssertionsForClassTypes.assertThat(resource).isNotNull();
        org.assertj.core.api.Assertions.assertThat(resource.getDelegations()).hasSize(1);
        DelegationWithInfo actual = resource.getDelegations().get(0);
        AssertionsForClassTypes.assertThat(actual.getId()).isEqualTo(expectedDelegation.getId());
        AssertionsForClassTypes.assertThat(actual.getInstitutionName()).isEqualTo(expectedDelegation.getInstitutionName());
        AssertionsForClassTypes.assertThat(actual.getInstitutionRootName()).isEqualTo(expectedDelegation.getInstitutionRootName());
        AssertionsForClassTypes.assertThat(actual.getBrokerName()).isEqualTo(expectedDelegation.getBrokerName());
        AssertionsForClassTypes.assertThat(actual.getBrokerId()).isEqualTo(expectedDelegation.getBrokerId());
        AssertionsForClassTypes.assertThat(actual.getProductId()).isEqualTo(expectedDelegation.getProductId());
        AssertionsForClassTypes.assertThat(actual.getInstitutionId()).isEqualTo(expectedDelegation.getInstitutionId());

        verify(delegationServiceMock, times(1))
                .getDelegationsV2(delegationParameters);
        verifyNoMoreInteractions(delegationServiceMock);
    }

    @Test
    void getDelegationsUsingTo_shouldGetData() throws Exception {
        // Given
        DelegationWithInfo expectedDelegation = dummyDelegation();
        PageInfo exptectedPageInfo = new PageInfo(10, 0, 1, 1);
        GetDelegationParameters delegationParameters = GetDelegationParameters.builder()
                .from(null)
                .to("to")
                .productId("prod-id")
                .search("search")
                .order(Order.ASC.name())
                .page(0)
                .size(10)
                .build();
        DelegationWithPagination expectedDelegationWithPagination = new DelegationWithPagination(List.of(expectedDelegation), exptectedPageInfo);

        when(delegationServiceMock.getDelegationsV2(delegationParameters)).thenReturn(expectedDelegationWithPagination);
        // When

        MvcResult result = mockMvc
                .perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/institutions?productId={productId}&search={search}&taxCode={taxCode}&mode=FULL&order=ASC&page={page}&size={size}",
                                delegationParameters.getTo(), delegationParameters.getProductId(), delegationParameters.getSearch(),
                                delegationParameters.getTaxCode(), delegationParameters.getPage(), delegationParameters.getSize()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andReturn();

        DelegationWithPagination resource = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        // Then
        AssertionsForClassTypes.assertThat(resource).isNotNull();
        org.assertj.core.api.Assertions.assertThat(resource.getDelegations()).hasSize(1);
        DelegationWithInfo actual = resource.getDelegations().get(0);
        AssertionsForClassTypes.assertThat(actual.getId()).isEqualTo(expectedDelegation.getId());
        AssertionsForClassTypes.assertThat(actual.getInstitutionName()).isEqualTo(expectedDelegation.getInstitutionName());
        AssertionsForClassTypes.assertThat(actual.getBrokerName()).isEqualTo(expectedDelegation.getBrokerName());
        AssertionsForClassTypes.assertThat(actual.getBrokerId()).isEqualTo(expectedDelegation.getBrokerId());
        AssertionsForClassTypes.assertThat(actual.getProductId()).isEqualTo(expectedDelegation.getProductId());
        AssertionsForClassTypes.assertThat(actual.getInstitutionId()).isEqualTo(expectedDelegation.getInstitutionId());

        verify(delegationServiceMock, times(1))
                .getDelegationsV2(delegationParameters);
        verifyNoMoreInteractions(delegationServiceMock);
    }

    /**
     * Method under test: {@link InstitutionV2Controller#getInstitutionOnboardingPending(String,String, String)}
     */
    @Test
    void getInstitutionOnboarding204() throws Exception {
        //given
        final String taxCode = "taxCode";
        final String productId = "productId";

        doReturn(Boolean.FALSE).when(institutionV2ServiceMock).verifyIfExistsPendingOnboarding(taxCode, null, productId);
        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/onboardings/{productId}/pending",  productId)
                        .queryParam("taxCode", taxCode)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent())
                .andReturn();
        //then
        verify(institutionV2ServiceMock, times(1))
                .verifyIfExistsPendingOnboarding(taxCode, null, productId);
        verifyNoMoreInteractions(institutionV2ServiceMock);

    }

    @Test
    void getInstitutionOnboarding200() throws Exception {
        //given
        final String taxCode = "taxCode";
        final String subunitCode = "subunitCode";
        final String productId = "productId";

        doReturn(Boolean.TRUE).when(institutionV2ServiceMock).verifyIfExistsPendingOnboarding(taxCode, subunitCode, productId);
        //when
        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/onboardings/{productId}/pending", productId)
                        .queryParam("taxCode", taxCode)
                        .queryParam("subunitCode", subunitCode)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        //then
        verify(institutionV2ServiceMock, times(1))
                .verifyIfExistsPendingOnboarding(taxCode, subunitCode, productId);
        verifyNoMoreInteractions(institutionV2ServiceMock);

    }

    @Test
    void getUserCount() throws Exception {
        final String institutionId = "institutionId";
        final String productId = "productId";
        final String[] roles = { MANAGER.name(),  DELEGATE.name() };
        final String[] status = { PENDING.name(), PENDING.name() };
        final UsersCountResponse userCount = getUsersCountResponse();
        when(userServiceMock.getUserCount(institutionId, productId, Arrays.asList(roles), Arrays.asList(status))).thenReturn(userCount);

        final MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/products/{productId}/users/count", institutionId, productId)
                        .queryParam("roles",roles)
                        .queryParam("status", status)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        UserCountResource resource = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        assertEquals(userCount.getInstitutionId(), resource.getInstitutionId());
        assertEquals(userCount.getProductId(), resource.getProductId());
        assertEquals(userCount.getRoles().get(0).getValue(), resource.getRoles().get(0));
        assertEquals(userCount.getRoles().get(1).getValue(), resource.getRoles().get(1));
        assertEquals(userCount.getStatus().get(0).getValue(), resource.getStatus().get(0));
        assertEquals(userCount.getStatus().get(1).getValue(), resource.getStatus().get(1));
        assertEquals(userCount.getCount(), resource.getCount());

        verify(userServiceMock, times(1))
                .getUserCount(institutionId, productId, Arrays.asList(roles), Arrays.asList(status));
        verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getUserCountWithNullRolesAndStatus() throws Exception {
        final String institutionId = "institutionId";
        final String productId = "productId";

        final UsersCountResponse userCount = getUsersCountResponse();
        when(userServiceMock.getUserCount(institutionId, productId, Collections.emptyList(), Collections.emptyList())).thenReturn(userCount);

        final MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/products/{productId}/users/count", institutionId, productId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        UserCountResource resource = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {});
        assertEquals(userCount.getInstitutionId(), resource.getInstitutionId());
        assertEquals(userCount.getProductId(), resource.getProductId());
        assertEquals(userCount.getRoles().get(0).getValue(), resource.getRoles().get(0));
        assertEquals(userCount.getRoles().get(1).getValue(), resource.getRoles().get(1));
        assertEquals(userCount.getStatus().get(0).getValue(), resource.getStatus().get(0));
        assertEquals(userCount.getStatus().get(1).getValue(), resource.getStatus().get(1));
        assertEquals(userCount.getCount(), resource.getCount());

        verify(userServiceMock, times(1))
                .getUserCount(institutionId, productId, Collections.emptyList(), Collections.emptyList());
        verifyNoMoreInteractions(userServiceMock);
    }

    @Test
    void getOnboardingsInfo() throws Exception {
        final String institutionId = "institutionId";
        final String[] products = { PROD_IO.name(),  PROD_IO_PREMIUM.name() };
        final OnboardingsResponse onboardingsResponse = getOnboardingsResponse();

        when(institutionV2ServiceMock.getOnboardingsInfoResponse(institutionId, Arrays.asList(products))).thenReturn(onboardingsResponse);

        final MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/onboardings-info", institutionId)
                        .queryParam("products", products)
                        .contentType(APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<OnboardingInfo> resource = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});

        assertEquals(onboardingsResponse.getOnboardings().get(0).getProductId(), resource.get(0).getProductId());
        assertEquals(onboardingsResponse.getOnboardings().get(0).getStatus().getValue(), resource.get(0).getStatus());
        assertEquals(onboardingsResponse.getOnboardings().get(0).getOrigin(), resource.get(0).getOrigin());
        assertEquals(onboardingsResponse.getOnboardings().get(0).getOriginId(), resource.get(0).getOriginId());
        assertEquals(onboardingsResponse.getOnboardings().get(0).getInstitutionType().getValue(), resource.get(0).getInstitutionType().name());
        assertTrue(resource.get(0).getContractAvailable());
        assertEquals(onboardingsResponse.getOnboardings().get(1).getProductId(), resource.get(1).getProductId());
        assertEquals(onboardingsResponse.getOnboardings().get(1).getStatus().getValue(), resource.get(1).getStatus());
        assertEquals(onboardingsResponse.getOnboardings().get(1).getOrigin(), resource.get(1).getOrigin());
        assertEquals(onboardingsResponse.getOnboardings().get(1).getOriginId(), resource.get(1).getOriginId());
        assertEquals(onboardingsResponse.getOnboardings().get(1).getInstitutionType().getValue(), resource.get(1).getInstitutionType().name());
        assertFalse(resource.get(1).getContractAvailable());

        verify(institutionV2ServiceMock, times(1))
                .getOnboardingsInfoResponse(institutionId, Arrays.asList(products));
        verifyNoMoreInteractions(institutionV2ServiceMock);
    }

    @Test
    void getContract() throws Exception {
        String institutionId = "institutionId";
        String productId = "productId";
        String text = "text";
        byte[] bytes = text.getBytes();
        InputStream is = new ByteArrayInputStream(bytes);

        Resource resource = Mockito.mock(Resource.class);
        when(resource.getInputStream()).thenReturn(is);
        when(resource.getFilename()).thenReturn("contract.pdf");

        when(institutionV2ServiceMock.getContract(institutionId, productId)).thenReturn(resource);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/contract", institutionId)
                        .queryParam("productId", productId)
                        .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(content().bytes(bytes))
                .andExpect(header().string("Content-Disposition", "attachment; filename=contract.pdf"));

        verify(institutionV2ServiceMock, times(1)).getContract(institutionId, productId);
    }

    @Test
    void checkUser() throws Exception {
        String institutionId = "institutionId";
        String productId = "productId";
        String fiscalCode = "fiscalCode";
        SearchUserDto userDto = new SearchUserDto();
        userDto.setFiscalCode(fiscalCode);

        when(userServiceMock.checkUser(fiscalCode, institutionId, productId)).thenReturn(Boolean.TRUE);

        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/{institutionId}/product/{productId}/check-user", institutionId, productId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(userDto)))
                        .andExpect(status().isOk())
                        .andExpect(content().string("{\"isUserOnboarded\":true}"))
                        .andReturn();

        verify(userServiceMock, times(1)).checkUser(fiscalCode, institutionId, productId);
    }

    @Test
    void checkUser_nullFiscalCode() throws Exception {
        String institutionId = "institutionId";
        String productId = "productId";
        SearchUserDto userDto = new SearchUserDto();

        mockMvc.perform(MockMvcRequestBuilders
                        .post(BASE_URL + "/{institutionId}/product/{productId}/check-user", institutionId, productId)
                        .contentType(APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest())
                .andReturn();

    }

    @Test
    void checkAttachment_true() throws Exception {
        String institutionId = "institutionId";
        String productId = "productId";
        String name = "name";

        when(institutionV2ServiceMock.checkAttachmentStatus(institutionId, productId, name)).thenReturn(Boolean.TRUE);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/product/{productId}/attachment/status", institutionId, productId)
                        .queryParam("name", name))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"isAttachmentAvailable\":true}"))
                .andReturn();

        verify(institutionV2ServiceMock, times(1)).checkAttachmentStatus(institutionId, productId, name);
    }

    @Test
    void checkAttachment_false() throws Exception {
        String institutionId = "institutionId";
        String productId = "productId";
        String name = "name";

        when(institutionV2ServiceMock.checkAttachmentStatus(institutionId, productId, name)).thenReturn(Boolean.FALSE);

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/product/{productId}/attachment/status", institutionId, productId)
                        .queryParam("name", name))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"isAttachmentAvailable\":false}"))
                .andReturn();

        verify(institutionV2ServiceMock, times(1)).checkAttachmentStatus(institutionId, productId, name);
    }

    @Test
    void checkAttachment_noName() throws Exception {
        String institutionId = "institutionId";
        String productId = "productId";

        mockMvc.perform(MockMvcRequestBuilders
                        .get(BASE_URL + "/{institutionId}/product/{productId}/attachment/status", institutionId, productId))
                .andExpect(status().isBadRequest())
                .andReturn();

    }

    private static UsersCountResponse getUsersCountResponse() {
        final List<it.pagopa.selfcare.user.generated.openapi.v1.dto.PartyRole> expectedRoles = List.of(MANAGER, DELEGATE);
        final List<OnboardedProductState> expectedStatus = List.of(OnboardedProductState.PENDING, OnboardedProductState.ACTIVE);

        final UsersCountResponse userCount = new UsersCountResponse();
        userCount.setInstitutionId("institutionId");
        userCount.setProductId("productId");
        userCount.setRoles(expectedRoles);
        userCount.setStatus(expectedStatus);
        userCount.setCount(2L);
        return userCount;
    }


    private DelegationWithInfo dummyDelegation() {
        DelegationWithInfo delegation = new DelegationWithInfo();
        delegation.setInstitutionId("from");
        delegation.setBrokerId("to");
        delegation.setId("setId");
        delegation.setProductId("setProductId");
        delegation.setType(DelegationType.PT);
        delegation.setInstitutionName("setInstitutionFromName");
        delegation.setBrokerName("setInstitutionFromRootName");
        return delegation;
    }

    private static OnboardingsResponse getOnboardingsResponse() {
        final OnboardingsResponse onboardingsResponse = new OnboardingsResponse();

        final OnboardingResponse onboardingResponse = new OnboardingResponse();
        onboardingResponse.setProductId(PROD_IO.name());
        onboardingResponse.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        onboardingResponse.setContract("contract");
        onboardingResponse.setOrigin("IPA");
        onboardingResponse.setOriginId("c_d277");
        onboardingResponse.setInstitutionType(OnboardingResponse.InstitutionTypeEnum.PT);

        final OnboardingResponse onboardingResponse1 = new OnboardingResponse();
        onboardingResponse1.setProductId(PROD_IO_PREMIUM.name());
        onboardingResponse1.setStatus(OnboardingResponse.StatusEnum.ACTIVE);
        onboardingResponse1.setOrigin("IPA");
        onboardingResponse1.setOriginId("c_d277");
        onboardingResponse1.setInstitutionType(OnboardingResponse.InstitutionTypeEnum.PT);
        onboardingResponse1.setCreatedAt(OffsetDateTime.of(2025,9,10,12,12,12,12, ZoneOffset.UTC));

        onboardingsResponse.setOnboardings(List.of(onboardingResponse, onboardingResponse1));

        return onboardingsResponse;
    }
}
