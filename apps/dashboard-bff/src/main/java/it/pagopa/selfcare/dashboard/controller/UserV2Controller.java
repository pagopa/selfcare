package it.pagopa.selfcare.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.dashboard.model.SearchUserDto;
import it.pagopa.selfcare.dashboard.model.UpdateUserDto;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapper;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperV2;
import it.pagopa.selfcare.dashboard.model.product.ProductUserResource;
import it.pagopa.selfcare.dashboard.model.user.*;
import it.pagopa.selfcare.dashboard.service.UserV2Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/v2/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "user")
public class UserV2Controller {

    private final UserV2Service userService;
    private final UserMapper userMapper;
    private final UserMapperV2 userMapperV2;

    @PostMapping(value = "/{userId}/suspend", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "suspendRelationship", description = "${swagger.dashboard.institutions.api.suspendUser}", operationId = "v2SuspendRelationshipUsingPOST")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:UpdateProductUsers')")
    public void suspendRelationship(@Parameter(description = "${swagger.dashboard.user.model.id}")
                                    @PathVariable("userId") String userId,
                                    @Parameter(description = "${swagger.dashboard.support.model.institutionId}")
                                    @RequestParam(value = "institutionId") String institutionId,
                                    @RequestParam(value = "productId") String productId,
                                    @RequestParam(value = "productRole", required = false) String productRole) {

        log.trace("suspendUser start");
        log.debug("suspendUser {} for institution: {}, productId: {} and productRole: {}", Encode.forJava(userId),
                Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(productRole));
        userService.suspendUserProduct(userId, institutionId, productId, productRole);
        log.trace("suspendUser end");

    }

    @PostMapping(value = "/{userId}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "activateRelationship", description = "${swagger.dashboard.institutions.api.activateUser}", operationId = "v2ActivateRelationshipUsingPOST")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:UpdateProductUsers')")
    public void activateRelationship(@Parameter(description = "${swagger.dashboard.user.model.id}")
                                     @PathVariable("userId") String userId,
                                     @RequestParam(value = "institutionId") String institutionId,
                                     @RequestParam(value = "productId") String productId,
                                     @RequestParam(value = "productRole", required = false) String productRole) {

        log.trace("activateUser start");
        log.debug("activateUser {} for institution: {}, productId: {} and productRole: {}", Encode.forJava(userId),
                Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(productRole));
        userService.activateUserProduct(userId, institutionId, productId, productRole);
        log.trace("activateUser end");

    }

    @DeleteMapping(value = "/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "deleteRelationshipById", description = "${swagger.dashboard.institutions.api.deleteUser}", operationId = "v2DeleteRelationshipByIdUsingDELETE")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:DeleteProductUsers')")
    public void deleteRelationshipById(@Parameter(description = "${swagger.dashboard.user.model.id}")
                                       @PathVariable("userId") String userId,
                                       @RequestParam(value = "institutionId") String institutionId,
                                       @RequestParam(value = "productId") String productId,
                                       @RequestParam(value = "productRole", required = false) String productRole) {
        log.trace("deleteUser start");
        log.debug("deleteUser {} for institution: {}, productId: {} and productRole: {}", Encode.forJava(userId),
                Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(productRole));
        userService.deleteUserProduct(userId, institutionId, productId, productRole);
        log.trace("deleteUser end");
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserById", description = "${swagger.dashboard.user.api.getUserByInternalId}", operationId = "v2GetUserByIdUsingGET")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:UpdateProductUsers')")
    public UserResource getUserById(@Parameter(description = "${swagger.dashboard.user.model.id}")
                                    @PathVariable("id")
                                    String userId,
                                    @Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                    @RequestParam(value = "institutionId")
                                    String institutionId,
                                    @Parameter(description = "${swagger.dashboard.user.model.fields}")
                                    @RequestParam(value = "fields", required = false)
                                    List<String> fields) {
        log.trace("getUserById start");
        log.debug("getUserById id = {}", Encode.forJava(userId));
        User user = userService.getUserById(userId, institutionId, fields);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserById = {}", user);
        log.trace("getUserById end");
        return userMapperV2.toUserResource(user);
    }

    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "search", description = "${swagger.dashboard.user.api.search}", operationId = "v2SearchUserByFiscalCodeUsingPOST")
    @ApiResponse(responseCode = "#404", description = "Not Found")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:CreateProductUsers')")
    public UserResource search(@Parameter(description = "${swagger.dashboard.user.model.searchUserDto}")
                               @RequestBody
                               @Valid
                               SearchUserDto searchUserDto,
                               @Parameter(description = "${swagger.dashboard.institutions.model.id}")
                               @RequestParam(value = "institutionId")
                               String institutionId) {
        log.trace("searchByFiscalCode start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "searchByFiscalCode fiscalCode = {}", searchUserDto);
        User user = userService.searchUserByFiscalCode(searchUserDto.getFiscalCode(), institutionId);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "searchByFiscalCode user = {}", user);
        log.trace("searchByFiscalCode end");
        return userMapperV2.toUserResource(user);
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "updateUser", description = "${swagger.dashboard.user.api.updateUserById}", operationId = "v2UpdateUserUsingPUT")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:UpdateProductUsers')")
    public void updateUser(@Parameter(description = "${swagger.dashboard.user.model.id}")
                           @PathVariable("id")
                           String userId,
                           @Parameter(description = "${swagger.dashboard.institutions.model.id}")
                           @RequestParam(value = "institutionId")
                           String institutionId,
                           @RequestBody
                           @Valid
                           UpdateUserDto updateUserDto) {
        log.trace("updateUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "userId = {}, institutionId = {}, userDto = {}", userId, institutionId, updateUserDto);
        userService.updateUser(userId, institutionId, userMapperV2.fromUpdateUser(updateUserDto));
        log.trace("updateUser end");
    }

    @GetMapping(value = "/institution/{institutionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUsers", description = "${swagger.dashboard.institutions.api.getInstitutionUsers}", operationId = "v2GetUsersUsingGET")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ListProductUsers')")
    public List<ProductUserResource> getUsers(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                              @PathVariable("institutionId") String institutionId,
                                              @RequestParam(value = "productId", required = false) String productId,
                                              @RequestParam(value = "productRoles", required = false) List<String> productRoles,
                                              @RequestParam(value = "roles", required = false) List<String> roles,
                                              Authentication authentication) {
        log.trace("getUsers start");
        log.debug("getUsers for institution: {} and product: {}", Encode.forJava(institutionId), Encode.forJava(productId));
        String loggedUserId = ((SelfCareUser) authentication.getPrincipal()).getId();

        List<ProductUserResource> response = new ArrayList<>();
        Collection<UserInfo> userInfos = userService.getUsersByInstitutionId(institutionId, productId, productRoles, roles, loggedUserId);
        userInfos.forEach(userInfo -> response.addAll(userMapper.toProductUsers(userInfo)));
        log.debug("getUsers result = {}", response);
        log.trace("getUsers end");

        return response;
    }

    @GetMapping(value = "/all/institution/{institutionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getAllUsers", description = "${swagger.dashboard.institutions.api.getInstitutionUsers}", operationId = "v2GetAllUsersUsingGET")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ListAllProductUsers')")
    public List<UserInstitutionRole> getAllUsers(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                                 @PathVariable("institutionId") String institutionId,
                                                 @RequestParam(value = "productId") String productId,
                                                 @RequestParam(value = "states", required = false) List<String> states,
                                                 @RequestParam(value = "roles", required = false) List<String> roles) {
        log.trace("getAllUsers start");
        log.debug("getAllUsers for institution: {} and product: {} with states: {} and roles: {}", Encode.forJava(institutionId), Encode.forJava(productId), Encode.forJava(states!=null ? states.toString(): null), Encode.forJava(roles != null ? roles.toString(): null));
        List<UserInstitutionRole> userRoles = userService.getAllUsersByInstitutionId(institutionId, productId, states, roles);
        log.debug("getAllUsers result = {}", userRoles);
        log.trace("getAllUsers end");
        return userRoles;
    }

    @GetMapping(value = "/otp-info")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserOtpEmailInfo", description = "${swagger.dashboard.user.api.getUserOtpEmailInfo}")
    public UserOtpEmailInfo getUserOtpEmailInfo(Authentication authentication) {
        final SelfCareUser selfCareUser = (SelfCareUser) authentication.getPrincipal();
        final String userId = selfCareUser.getId();
        log.debug("getUserOtpEmailInfo for userId: {}", Encode.forJava(userId));
        return userService.getUserOtpEmailInfo(userId);
    }

}
