package it.pagopa.selfcare.external_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.external_api.mapper.UserInfoResourceMapper;
import it.pagopa.selfcare.external_api.model.user.*;
import it.pagopa.selfcare.external_api.service.UserService;
import it.pagopa.selfcare.onboarding.common.PartyRole;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/v2/users", produces = APPLICATION_JSON_VALUE)
@Tag(name = "User")
public class UserV2Controller {

    private final UserService userService;
    private final UserInfoResourceMapper userInfoResourceMapper;

    public UserV2Controller(UserService userService, UserInfoResourceMapper userInfoResourceMapper) {
        this.userService = userService;
        this.userInfoResourceMapper = userInfoResourceMapper;
    }
    @Tag(name = "support")
    @Tag(name = "support-pnpg")
    @Tag(name = "external-v2")
    @Tag(name = "internal-v1")
    @Tag(name = "User")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserInfo", description = "${swagger.external_api.user.api.getUserInfo2}", operationId = "#V2getUserInfoUsingGET")
    public UserInfoResource getUserInfo(@Parameter(description = "${swagger.external_api.user.model.searchUser}")
                                        @RequestBody @Valid SearchUserDto searchUserDto,
                                        @RequestParam(value = "productId", required = false) String productId) {
        log.trace("getUserInfo start");
        UserInfoWrapper userInfo = userService.getUserInfoV2(searchUserDto.getFiscalCode(), searchUserDto.getStatuses(), productId);
        UserInfoResource userInfoResource = userInfoResourceMapper.toResource(userInfo);
        log.debug("getUserInfo result = {}", userInfoResource);
        log.trace("getUserInfo end");
        return userInfoResource;
    }

    @GetMapping(value = "/{id}/onboarded-product")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserProductInfo", description = "${swagger.external_api.user.api.getUserProductInfo}", operationId = "#V2getUserProductInfoUsingGET")
    public UserDetailsResource getUserProductInfo(@Parameter(description = "${swagger.external_api.user.model.id}")
                                                  @PathVariable("id") String userId,
                                                  @Parameter(description = "${swagger.external-api.product.model.id}")
                                                  @RequestParam("productId") String productId,
                                                  @Parameter(description = "${swagger.external_api.institutions.model.id}")
                                                  @RequestParam("institutionId")
                                                  String institutionId) {
        log.trace("getUserProductInfo start");
        UserDetailsResource userDetailsResource = userInfoResourceMapper.toResource(userService.getUserOnboardedProductsDetailsV2(userId, institutionId, productId));
        log.debug("getUserProductInfo result = {}", userDetailsResource);
        log.trace("getUserProductInfo end");
        return userDetailsResource;
    }


    @Tag(name = "external-v2")
    @Tag(name = "external-pnpg")
    @Tag(name = "User")
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserInstitution", description = "This endpoint retrieves detailed information about a user's association with various products within an institution. The response provides a comprehensive view of the user's roles, product statuses and the relevant timestamps.", operationId = "#v2getUserInstitution")
    public List<UserInstitutionResource> getUserInstitution(@Parameter(description = "${swagger.external_api.institutions.model.id}")
                                                      @RequestParam(value = "institutionId", required = false) String institutionId,
                                                      @Parameter(description = "${swagger.external_api.user.model.id}")
                                                      @RequestParam(value = "userId", required = false) String userId,
                                                      @Parameter(description = "${swagger.external_api.model.roles}")
                                                      @RequestParam(value = "roles", required = false) List<PartyRole> roles,
                                                      @Parameter(description = "${swagger.external_api.model.states}")
                                                      @RequestParam(value = "states", required = false) List<String> states,
                                                      @RequestParam(value = "products", required = false) List<String> products,
                                                      @Parameter(description = "${swagger.external_api.model.states}")
                                                      @RequestParam(value = "productRoles", required = false) List<String> productRoles,
                                                      @RequestParam(value = "page", defaultValue = "0") Integer page,
                                                      @RequestParam(value = "size", defaultValue = "100") Integer size) {

        log.trace("getUserInstitution start, institutionId: {}, userId: {}", Encode.forJava(institutionId), Encode.forJava(userId));
        List<UserInstitution> userInstitutions = userService.getUsersInstitutions(userId, institutionId, page, size, productRoles, products, roles, states);
        log.trace("getUserInstitution end");
        return userInstitutions.stream()
                .map(userInfoResourceMapper::toUserInstitutionResource)
                .toList();
    }
}
