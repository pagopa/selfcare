package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.user.constant.OnboardedProductState;
import it.pagopa.selfcare.user.controller.request.CreateUserDto;
import it.pagopa.selfcare.user.controller.response.UserDataResponse;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.product.SearchUserDto;
import it.pagopa.selfcare.user.entity.UserInfo;
import it.pagopa.selfcare.user.entity.UserInstitutionRole;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.model.notification.UserNotificationToSend;
import it.pagopa.selfcare.user.service.UserRegistryService;
import it.pagopa.selfcare.user.service.UserService;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.user_registry_json.model.CertifiableFieldResourceOfstring;
import org.openapi.quarkus.user_registry_json.model.MutableUserFieldsDto;
import org.openapi.quarkus.user_registry_json.model.UserResource;
import org.openapi.quarkus.user_registry_json.model.WorkContactResource;

import java.util.*;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(UserController.class)
class UserControllerTest {

    @InjectMock
    private UserService userService;
    @InjectMock
    private UserRegistryService userRegistryService;

    private static final UserResource userResource;

    static {
        userResource = new UserResource();
        userResource.setEmail(new CertifiableFieldResourceOfstring(CertifiableFieldResourceOfstring.CertificationEnum.NONE, "email"));
        userResource.setName(new CertifiableFieldResourceOfstring(CertifiableFieldResourceOfstring.CertificationEnum.NONE, "name"));
        userResource.setFamilyName(new CertifiableFieldResourceOfstring(CertifiableFieldResourceOfstring.CertificationEnum.NONE, "familyName"));
        userResource.setFiscalCode("fiscalCode");
        userResource.setWorkContacts(Map.of("userMailUuid", new WorkContactResource(new CertifiableFieldResourceOfstring(CertifiableFieldResourceOfstring.CertificationEnum.NONE, "email"))));

    }

    /**
     * Method under test: {@link UserController#getUsersEmailByInstitutionAndProduct(String, String)}}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void getUsersEmailByInstitution() {

        when(userService.getUsersEmails(anyString(), anyString()))
                .thenReturn(Uni.createFrom().item(List.of("test@test.it")));

        var institutionId = "institutionId";
        var productId = "productId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/emails?institutionId=" + institutionId +"&productId=" + productId)
                .then()
                .statusCode(200);
    }

    /**
     * Method under test: {@link UserController#getUsersEmailByInstitutionAndProduct(String, String)}}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void getUsersEmailWithNullInstitutionId() {

        var productId = "productId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/emails?productId=" + productId)
                .then()
                .statusCode(400);
    }

    /**
     * Method under test: {@link UserController#getUsersEmailByInstitutionAndProduct(String, String)}
     */
    @Test
    void getUsersEmailByInstitutionNotAuthorized() {

        var institutionId = "institutionId";
        var productId = "productId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/emails?institutionId=" + institutionId + "&productId=" + productId)
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testGetUserInfoOk() {
        UserResource userResource = new UserResource();
        userResource.setId(UUID.randomUUID());
        userResource.setFiscalCode("test");

        when(userService.retrievePerson(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(userResource));

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/test_user_id")
                .then()
                .statusCode(200);
    }

    @Test
    void testGetUserInfoNotAuthorized() {
        when(userService.retrievePerson(any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("test", "test")));

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/test_user_id")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testGetUserInfoFails() {
        when(userService.retrievePerson(any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("test", "test")));

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/test_user_id")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateUserStatus() {

        when(userService.updateUserStatusWithOptionalFilter("userId", null, "prod-pagopa", null, null, OnboardedProductState.ACTIVE))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("id", "userId")
                .queryParam("productId", "prod-pagopa")
                .queryParam("status", OnboardedProductState.ACTIVE)
                .put("/{id}/status")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getUserDetailsById(){

        when(userService.getUserById(any())).thenReturn(Uni.createFrom().item(userResource));

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/test_user_id/details")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void searchUser(){
        SearchUserDto dto = new SearchUserDto("fiscalCode");

        when(userService.searchUserByFiscalCode(any())).thenReturn(Uni.createFrom().item(userResource));
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(dto)
                .post("/search")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateUserStatusError() {

        when(userService.updateUserStatusWithOptionalFilter("userId", null, "prod-pagopa", null, null, OnboardedProductState.ACTIVE))
                .thenThrow(new ResourceNotFoundException("user non trovato"));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("id", "userId")
                .queryParam("productId", "prod-pagopa")
                .queryParam("status", OnboardedProductState.ACTIVE)
                .put("/{id}/status")
                .then()
                .statusCode(404);
    }

    /**
     * Method under test:
     * {@link UserController#deleteProducts(String, String, String)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void deleteDeleteProductsErrorTest() {
        String PATH_USER_ID = "userId";
        String PATH_INSTITUTION_ID = "institutionId";
        String PATH_PRODUCT_ID = "productId";
        String PATH_DELETE_PRODUCT = "{userId}/institutions/{institutionId}/products/{productId}";

        var user = "user1";
        var institution = "institution1";
        var product = "product1";
        when(userService.deleteUserInstitutionProduct("user1", "institution1", "product1"))
                .thenThrow(InvalidRequestException.class);

        given()
                .when()
                .pathParam(PATH_USER_ID, user)
                .pathParam(PATH_INSTITUTION_ID, institution)
                .pathParam(PATH_PRODUCT_ID, product)
                .delete(PATH_DELETE_PRODUCT)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Method under test:
     * {@link UserController#deleteProducts(String, String, String)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void deleteDeleteProductsOKTest() {

        String PATH_USER_ID = "userId";
        String PATH_INSTITUTION_ID = "institutionId";
        String PATH_PRODUCT_ID = "productId";
        String PATH_DELETE_PRODUCT = "{userId}/institutions/{institutionId}/products/{productId}";

        var user = "user123";
        var institution = "institution123";
        var product = "prod-pagopa";

        when(userService.deleteUserInstitutionProduct("user123", "institution123", "prod-pagopa"))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .pathParam(PATH_USER_ID, user)
                .pathParam(PATH_INSTITUTION_ID, institution)
                .pathParam(PATH_PRODUCT_ID, product)
                .delete(PATH_DELETE_PRODUCT)
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testGetUserProductsInfoOk() {
        UserInfo userInfoResponse = new UserInfo();
        userInfoResponse.setUserId("test-user");

        UserInstitutionRole userInstitution = new UserInstitutionRole();
        userInstitution.setInstitutionName("test-institutionId");
        userInstitution.setStatus(OnboardedProductState.ACTIVE);

        List<UserInstitutionRole> userInstitutionRoleResponses = new ArrayList<>();
        userInstitutionRoleResponses.add(userInstitution);
        userInfoResponse.setInstitutions(userInstitutionRoleResponses);

        when(userService.retrieveBindings(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(userInfoResponse));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("userId", "test-user-id")
                .get("/{userId}/institutions")
                .then()
                .statusCode(200);

        Mockito.verify(userService).retrieveBindings(any(), any(), any());

    }

    @Test
    void testGetUserProductsInfoNotAuthorized() {

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("userId", "test-user-id")
                .get("/{userId}/institutions")
                .then()
                .statusCode(401);

        Mockito.verify(userService, Mockito.never()).retrieveBindings(any(), any(), any());
    }


    @Test

    @TestSecurity(user = "userJwt")
    void findByIds(){
        String PATH_RETRIEVE_ALL_USERS_BY_IDS = "/ids";
        List<String> userIds = List.of("user1");
        when(userService.findAllByIds(any())).thenReturn(
                Uni.createFrom().item(List.of(new UserInstitutionResponse())));
        given()
                .when()
                .queryParam("userIds", userIds)
                .get(PATH_RETRIEVE_ALL_USERS_BY_IDS)
                .then()
                .statusCode(HttpStatus.SC_OK);

    }

    @Test
    void testGetUsersNotAuthorized() {
        var productId = "productId";

        given().when()
                .contentType(ContentType.JSON)
                .queryParam("page", 0)
                .queryParam("size", 100)
                .queryParam(productId, "productId")
                .get("/notification")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testGetUsers() {
        when(userService.findPaginatedUserNotificationToSend(0, 100, "productId"))
                .thenReturn(Uni.createFrom().item( List.of(new UserNotificationToSend())));

        var productId = "productId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .queryParam("page", 0)
                .queryParam("size", 100)
                .queryParam(productId, "productId")
                .get("/notification")
                .then()
                .statusCode(200);
    }


    /**
     * Method under test: {@link InstitutionController#retrieveUsers(String, String, List, List, List, List))}
     */
    @Test
    void testGetUserInstitutionsNotAuthorized() {
        given()
                .when()
                .contentType(ContentType.JSON)
                .get()
                .then()
                .statusCode(401);
    }

    /**
     * Method under test: {@link InstitutionController#retrieveUsers(String, String, List, List, List, List))}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void testGetUserInstitutions() {
        Map<String, Object> queryParam = new HashMap<>();
        queryParam.put("institutionId", "Id");
        queryParam.put("userId", "userId");
        when(userService.findPaginatedUserInstitutions("Id",  "userId", null, null, null, null, 0 , 100))
                .thenReturn(Multi.createFrom().items(new UserInstitutionResponse()));

        given()
                .when()
                .queryParams(queryParam)
                .contentType(ContentType.JSON)
                .get("")
                .then()
                .statusCode(200);
    }

    @Test
    void testUpdateUserRegistryAndSendNotificationToQueueUnauthorized() {
        given()
                .when()
                .contentType(ContentType.JSON)
                .put("/test_user_id/user-registry?institutionId=institutionIdTest")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testUpdateUserRegistryAndSendNotificationToQueue() {
        MutableUserFieldsDto mutableUserFieldsDto = new MutableUserFieldsDto();
        when(userRegistryService.updateUserRegistryAndSendNotificationToQueue(mutableUserFieldsDto, "test_user_id", "institutionIdTest")).thenReturn(Uni.createFrom().nullItem());
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(mutableUserFieldsDto)
                .put("/test_user_id/user-registry?institutionId=institutionIdTest")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateUserProductsErrorTest() {
        String PATH_USER_ID = "id";
        String PATH_INSTITUTION_ID = "institutionId";
        String PATH_PRODUCT_ID = "productId";
        String PATH_UPDATE_PRODUCT = "{id}/institution/{institutionId}/product/{productId}/status";

        var user = "user1";
        var institution = "institution1";
        var product = "product1";
        Mockito.when(userService.updateUserProductStatus("user1","institution1", "product1", OnboardedProductState.ACTIVE,null))
                .thenThrow(InvalidRequestException.class);

        given()
                .when()
                .pathParam(PATH_USER_ID, user)
                .pathParam(PATH_INSTITUTION_ID, institution)
                .pathParam(PATH_PRODUCT_ID, product)
                .queryParam("status", OnboardedProductState.ACTIVE)
                .put(PATH_UPDATE_PRODUCT)
                .then()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Method under test:
     * {@link UserController#deleteProducts(String, String, String)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void updateUserProductsOKTest() {

        String PATH_USER_ID = "id";
        String PATH_INSTITUTION_ID = "institutionId";
        String PATH_PRODUCT_ID = "productId";
        String PATH_UPDATE_PRODUCT = "{id}/institution/{institutionId}/product/{productId}/status";

        var user = "user123";
        var institution = "institution123";
        var product = "prod-pagopa";

        Mockito.when(userService.updateUserProductStatus(eq("user123"),eq("institution123"), eq("prod-pagopa"), eq(OnboardedProductState.ACTIVE), any()))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .pathParam(PATH_USER_ID, user)
                .pathParam(PATH_INSTITUTION_ID, institution)
                .pathParam(PATH_PRODUCT_ID, product)
                .queryParam("status", OnboardedProductState.ACTIVE)
                .put(PATH_UPDATE_PRODUCT)
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testCreateOrUpdateUser() {
        // Prepare test data
        CreateUserDto userDto = buildCreateUserDto();


        // Mock the userService.createOrUpdateUser method
        when(userService.createOrUpdateUser(any(CreateUserDto.class)))
                .thenReturn(Uni.createFrom().nullItem());

        // Perform the API call
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(userDto)
                .post("/")
                .then()
                .statusCode(204);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void testCreateOrUpdateUserWithInvalidBody() {
        // Prepare test data
        CreateUserDto userDto = new CreateUserDto();
        // Set userDto properties

        // Mock the userService.createOrUpdateUser method
        when(userService.createOrUpdateUser(any(CreateUserDto.class)))
                .thenReturn(Uni.createFrom().nullItem());

        // Perform the API call
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(userDto)
                .post("/")
                .then()
                .statusCode(400);
    }

    @Test
    void testCreateOrUpdateUserNotAuthorized() {
        // Prepare test data
        CreateUserDto userDto = new CreateUserDto();
        // Set userDto properties

        // Perform the API call
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(userDto)
                .post("/")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void retrieveUsers() {
        when(userService.retrieveUsersData("test_institutionId",  null, null, null, null, null, "test_userId"))
                .thenReturn(Multi.createFrom().items(new UserDataResponse()));

        given()
                .when()
                .contentType(ContentType.JSON)
                .get("/test_userId/institution/test_institutionId")
                .then()
                .statusCode(200);
    }


    private CreateUserDto buildCreateUserDto() {
        CreateUserDto userDto = new CreateUserDto();
        userDto.setInstitutionId("institutionId");
        userDto.setInstitutionDescription("institutionDescription");
        userDto.setInstitutionRootName("institutionRootName");
        CreateUserDto.User user = new CreateUserDto.User();
        user.setBirthDate("birthDate");
        user.setFamilyName("familyName");
        user.setFiscalCode("fiscalCode");
        user.setName("name");
        user.setInstitutionEmail("institutionEmail");
        userDto.setUser(user);
        CreateUserDto.Product product = new CreateUserDto.Product();
        product.setProductId("productId");
        product.setRole(it.pagopa.selfcare.onboarding.common.PartyRole.MANAGER);
        product.setTokenId("tokenId");
        product.setProductRole("productRole");
        userDto.setProduct(product);
        return userDto;
    }
}