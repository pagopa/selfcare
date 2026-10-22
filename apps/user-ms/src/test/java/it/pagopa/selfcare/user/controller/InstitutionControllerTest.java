package it.pagopa.selfcare.user.controller;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.user.controller.request.UpdateDescriptionDto;
import it.pagopa.selfcare.user.controller.request.UpdateProductUsersDto;
import it.pagopa.selfcare.user.controller.response.DeletedUserCountResponse;
import it.pagopa.selfcare.user.controller.response.UserInstitutionResponse;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;
import it.pagopa.selfcare.user.controller.response.UsersCountResponse;
import it.pagopa.selfcare.user.controller.response.product.SearchUserDto;
import it.pagopa.selfcare.user.exception.InvalidRequestException;
import it.pagopa.selfcare.user.exception.ResourceNotFoundException;
import it.pagopa.selfcare.user.model.LoggedUser;
import it.pagopa.selfcare.user.model.constants.OnboardedProductState;
import it.pagopa.selfcare.user.service.UserService;
import it.pagopa.selfcare.user.util.UserUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@QuarkusTest
@TestHTTPEndpoint(InstitutionController.class)
class InstitutionControllerTest {

    @InjectMock
    private UserService userService;

    @InjectMock
    private UserUtils userUtils;

    private static UpdateProductUsersDto buildUpdateProductUsersDto(List<UpdateProductUsersDto.ProductUser> users) {
        UpdateProductUsersDto dto = new UpdateProductUsersDto();
        dto.setInstitutionDescription("institutionDescription");
        dto.setTokenId("tokenId");
        dto.setUsers(users);
        return dto;
    }

    private static UpdateProductUsersDto.ProductUser buildProductUser(String userId, String role) {
        UpdateProductUsersDto.ProductUser user = new UpdateProductUsersDto.ProductUser();
        user.setUserId(userId);
        user.setRole(role);
        user.setProductRoles(List.of("admin"));
        user.setUserMailUuid("userMailUuid");
        return user;
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateProductUsers() {
        final String institutionId = "institutionId";
        final String productId = "prod-io";
        UpdateProductUsersDto dto = buildUpdateProductUsersDto(List.of(buildProductUser("userId", "MANAGER")));
        LoggedUser loggedUser = LoggedUser.builder().uid("uid").name("name").familyName("familyName").build();

        Mockito.when(userUtils.readUserIdFromToken(any())).thenReturn(Uni.createFrom().item(loggedUser));
        Mockito.when(userService.updateProductUsers(eq(institutionId), eq(productId), any(UpdateProductUsersDto.class), eq(loggedUser)))
                .thenReturn(Uni.createFrom().voidItem());

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .body(dto)
                .put("/{institutionId}/products/{productId}/users")
                .then()
                .statusCode(204);

        verify(userService).updateProductUsers(eq(institutionId), eq(productId), any(UpdateProductUsersDto.class), eq(loggedUser));
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateProductUsers_emptyUsers() {
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", "institutionId")
                .pathParam("productId", "prod-io")
                .body(buildUpdateProductUsersDto(List.of()))
                .put("/{institutionId}/products/{productId}/users")
                .then()
                .statusCode(400);

        Mockito.verifyNoInteractions(userService);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void updateProductUsers_invalidRole() {
        Mockito.when(userUtils.readUserIdFromToken(any())).thenReturn(Uni.createFrom().item(LoggedUser.builder().uid("uid").build()));
        Mockito.when(userService.updateProductUsers(anyString(), anyString(), any(UpdateProductUsersDto.class), any(LoggedUser.class)))
                .thenReturn(Uni.createFrom().failure(new InvalidRequestException("Invalid role")));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", "institutionId")
                .pathParam("productId", "prod-io")
                .body(buildUpdateProductUsersDto(List.of(buildProductUser("userId", "OPERATOR"))))
                .put("/{institutionId}/products/{productId}/users")
                .then()
                .statusCode(400);
    }

    @Test
    void updateProductUsers_notAuthorized() {
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", "institutionId")
                .pathParam("productId", "prod-io")
                .body(buildUpdateProductUsersDto(List.of(buildProductUser("userId", "MANAGER"))))
                .put("/{institutionId}/products/{productId}/users")
                .then()
                .statusCode(401);
    }

    /**
     * Method under test: {@link InstitutionController#getInstitutionUsers(String, List, List, String)}
     */
    @Test
    void getInstitutionUsersNotAuthorized() {

        var institutionId = "institutionId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/{institutionId}/users")
                .then()
                .statusCode(401);
    }

    /**
     * Method under test: {@link InstitutionController#getInstitutionUsers(String, List, List, String) }
     */
    @Test
    @TestSecurity(user = "userJwt")
    void getInstitutionUsers() {

        Mockito.when(userService.getUserProductsByInstitution(anyString(), any(), any(), anyString()))
                .thenReturn(Multi.createFrom().item(new UserProductResponse()));

        var institutionId = "institutionId";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/{institutionId}/users")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getInstitutionUsers_withFilters() {

        Mockito.when(userService.getUserProductsByInstitution(anyString(), any(), any(), anyString()))
                .thenReturn(Multi.createFrom().item(new UserProductResponse()));

        String institutionId = "institutionId";
        String userId= "userId";
        List<String> products= List.of("prod-io");
        List<String> roles = List.of("MANAGER", "DELEGATE");

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .queryParam("products", products)
                .queryParam("roles", roles)
                .queryParam("userId", userId)
                .get("/{institutionId}/users")
                .then()
                .statusCode(200);

        verify(userService).getUserProductsByInstitution(institutionId, products, roles, userId);

    }

    /**
     * Method under test: {@link InstitutionController#retrieveUserInstitutions(String, String, List, List, List, List)}
     */
    @Test
    void testGetUserInstitutionsNotAuthorized() {
        var institutionId = "institutionId";
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/{institutionId}/user-institutions")
                .then()
                .statusCode(401);
    }

    /**
     * Method under test: {@link InstitutionController#retrieveUserInstitutions(String, String, List, List, List, List)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void testGetUserInstitutions() {
        var institutionId = "institutionId";
        Mockito.when(userService.findAllUserInstitutions(any(), any(), any(), any(), any(), any()))
                .thenReturn(Multi.createFrom().items(new UserInstitutionResponse()));
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .get("/{institutionId}/user-institutions?products=1,2")
                .then()
                .statusCode(200);
    }

    /**
     * Method under test: {@link InstitutionController#updateUserProductCreatedAt(String, String, List, OffsetDateTime)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void updateUserProductCreatedAt() {

        var institutionId = "institutionId";
        var productId = "prod-io";
        var now = OffsetDateTime.now();
        Mockito.when(userService.updateUserProductCreatedAt(institutionId, List.of("userId"), productId, now))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .put("/{institutionId}/products/{productId}/created-at?userIds=userId&createdAt=" + now)
                .then()
                .statusCode(204);
    }

    /**
     * Method under test: {@link InstitutionController#updateUserProductCreatedAt(String, String, List, OffsetDateTime)}
     */
    @Test
    void updateUserProductCreatedAt_NotAuthorized() {

        var institutionId = "institutionId";
        var productId = "prod-pagopa";
        var now = OffsetDateTime.now();
        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .put("/{institutionId}/products/{productId}/created-at?createdAt=" + now)
                .then()
                .statusCode(401);
    }

    /**
     * Method under test: {@link InstitutionController#updateUserProductCreatedAt(String, String, List, OffsetDateTime)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void updateUserProductCreatedAt_UserNotFound() {
        final OffsetDateTime now = OffsetDateTime.now();
        final String institutionId = "institutionId";
        final String productId = "prod-io";
        final String userId = "userId";
        Mockito.when(userService.updateUserProductCreatedAt(institutionId, List.of(userId), productId, now))
                .thenThrow(new ResourceNotFoundException("user non trovato"));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .put("/{institutionId}/products/{productId}/created-at?userIds=" + userId + "&createdAt=" + now)
                .then()
                .statusCode(404);
    }

    /**
     * Method under test: {@link InstitutionController#updateInstitutionDescription(String, UpdateDescriptionDto)}
     */
    @Test
    @TestSecurity(user = "userJwt")
    void updateInstitutionDescription() {

        var institutionId = "institutionId";
        UpdateDescriptionDto updateDescriptionDto = new UpdateDescriptionDto();
        updateDescriptionDto.setInstitutionDescription("description");

        Mockito.when(userService.updateInstitutionDescription(institutionId, updateDescriptionDto))
                .thenReturn(Uni.createFrom().nullItem());

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(updateDescriptionDto)
                .pathParam("institutionId", institutionId)
                .put("/{institutionId}")
                .then()
                .statusCode(204);
    }

    @Test
    void updateInstitutionDescription_NotAuthorized() {

        var institutionId = "institutionId";
        UpdateDescriptionDto updateDescriptionDto = new UpdateDescriptionDto();
        updateDescriptionDto.setInstitutionDescription("description");

        given()
                .when()
                .contentType(ContentType.JSON)
                .body(updateDescriptionDto)
                .pathParam("institutionId", institutionId)
                .put("/{institutionId}")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void getUsersCount() {
        final String institutionId = "institutionId";
        final String productId = "prod-pagopa";
        final List<PartyRole> roles = List.of(PartyRole.MANAGER, PartyRole.DELEGATE);
        final List<OnboardedProductState> status = List.of(OnboardedProductState.ACTIVE, OnboardedProductState.PENDING);

        Mockito.when(userService.getUsersCount(institutionId, productId, roles, status))
                .thenReturn(Uni.createFrom().item(new UsersCountResponse(institutionId, productId, roles, status, 2L)));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .queryParam("roles", List.of("MANAGER", "DELEGATE"))
                .queryParam("status", List.of("ACTIVE", "PENDING"))
                .get("/{institutionId}/products/{productId}/users/count")
                .then()
                .statusCode(200);
    }

    @Test
    void getUsersCount_NotAuthorized() {
        final String institutionId = "institutionId";
        final String productId = "prod-io";
        final List<String> roles = List.of("role1", "role2");
        final List<String> status = List.of("status1", "status2");

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .queryParam("roles", roles)
                .queryParam("status", status)
                .get("/{institutionId}/products/{productId}/users/count")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void deleteUserInstitutionProductUsers() {
        final String institutionId = "institutionId";
        final String productId = "prod-io";

        Mockito.when(userService.deleteUserInstitutionProductUsers(institutionId, productId))
                .thenReturn(Uni.createFrom().item(new DeletedUserCountResponse(institutionId, productId, 1L)));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .delete("/{institutionId}/products/{productId}/users")
                .then()
                .statusCode(200);
    }

    @Test
    void deleteUserInstitutionProductUsers_NotAuthorized() {
        final String institutionId = "institutionId";
        final String productId = "prod-pagopa";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .delete("/{institutionId}/products/{productId}/users")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "userJwt")
    void checkUser() {
        final String fiscalCode = "fiscalCode";
        final String institutionId = "institutionId";
        final String productId = "prod-pagopa";

        Mockito.when(userService.checkUser(fiscalCode, institutionId, productId))
                .thenReturn(Uni.createFrom().item(Boolean.TRUE));

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .body(new SearchUserDto(fiscalCode))
                .post("/{institutionId}/product/{productId}/check-user")
                .then()
                .statusCode(200);
    }

    @Test
    void checkUser_NotAuthorized() {
        final String fiscalCode = "fiscalCode";
        final String institutionId = "institutionId";
        final String productId = "prod-pagopa";

        given()
                .when()
                .contentType(ContentType.JSON)
                .pathParam("institutionId", institutionId)
                .pathParam("productId", productId)
                .body(new SearchUserDto(fiscalCode))
                .post("/{institutionId}/product/{productId}/check-user")
                .then()
                .statusCode(401);
    }

}
