package it.pagopa.selfcare.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.OnboardingsResponse;
import it.pagopa.selfcare.dashboard.aspect.ApiFeatureFlag;
import it.pagopa.selfcare.dashboard.model.CreateUserDto;
import it.pagopa.selfcare.dashboard.model.*;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationWithPagination;
import it.pagopa.selfcare.dashboard.model.delegation.GetDelegationParameters;
import it.pagopa.selfcare.dashboard.model.delegation.Order;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.institution.InstitutionBase;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionResourceMapper;
import it.pagopa.selfcare.dashboard.model.mapper.OnboardingMapper;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapper;
import it.pagopa.selfcare.dashboard.model.mapper.UserMapperV2;
import it.pagopa.selfcare.dashboard.model.user.*;
import it.pagopa.selfcare.dashboard.service.DelegationService;
import it.pagopa.selfcare.dashboard.service.InstitutionV2Service;
import it.pagopa.selfcare.dashboard.service.UserV2Service;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserProductResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UsersCountResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.owasp.encoder.Encode;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;


@Slf4j
@RestController
@RequestMapping(value = "/v2/institutions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "institutions")
@RequiredArgsConstructor
public class InstitutionV2Controller {

    private final InstitutionV2Service institutionV2Service;
    private final UserV2Service userService;
    private final InstitutionResourceMapper institutionResourceMapper;
    private final UserMapper userMapper;
    private final UserMapperV2 userMapperV2;
    private final OnboardingMapper onboardingMapper;
    private final DelegationService delegationService;

    @GetMapping(value = "/{institutionId}/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getInstitutionUser", description = "${swagger.dashboard.institutions.api.getInstitutionUser}", operationId = "#v2RetrieveInstitutionUser")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:ListProductUsers')")
    public InstitutionUserDetailsResource getInstitutionUser(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                                             @PathVariable("institutionId")
                                                             String institutionId,
                                                             @Parameter(description = "${swagger.dashboard.user.model.id}")
                                                             @PathVariable("userId")
                                                             String userId,
                                                             Authentication authentication) {

        log.trace("getInstitutionUser start");
        String loggedUserId = ((SelfCareUser) authentication.getPrincipal()).getId();

        log.debug("getInstitutionUser institutionId = {}, userId = {}", Encode.forJava(institutionId), Encode.forJava(userId));
        UserInfo userInfo = institutionV2Service.getInstitutionUser(institutionId, userId, loggedUserId);
        InstitutionUserDetailsResource result = userMapper.toInstitutionUserDetails(userInfo);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionUser result = {}", result);
        log.trace("getInstitutionUser end");
        return result;
    }

    @GetMapping(value = "/all/{institutionId}/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getAllInstitutionUser", description = "${swagger.dashboard.institutions.api.getAllInstitutionUser}", operationId = "#v2RetrieveAllInstitutionUser")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:ListAllProductUsers')")
    public InstitutionUserDetailsResource getAllInstitutionUser(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                                                @PathVariable("institutionId")
                                                                String institutionId,
                                                                @Parameter(description = "${swagger.dashboard.user.model.id}")
                                                                @PathVariable("userId")
                                                                String userId,
                                                                Authentication authentication) {

        final String loggedUserId = ((SelfCareUser) authentication.getPrincipal()).getId();
        log.debug("getAllInstitutionUser institutionId = {}, userId = {}, loggedUserId = {}", Encode.forJava(institutionId), Encode.forJava(userId), Encode.forJava(loggedUserId));
        final UserProductResponse userProductResponse = institutionV2Service.getAllInstitutionUser(institutionId, userId, loggedUserId);
        return institutionResourceMapper.toInstitutionUserDetailsResponse(userProductResponse);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getInstitutions", description = "${swagger.dashboard.institutions.api.getInstitutions}", operationId = "#v2RetrieveUserInstitutions")
    public List<InstitutionBaseResource> getInstitutions(Authentication authentication) {

        log.trace("getInstitutions start");
        String userId = ((SelfCareUser) authentication.getPrincipal()).getId();
        Collection<InstitutionBase> institutions = userService.getInstitutions(userId);

        List<InstitutionBaseResource> result = institutions.stream()
                .map(institutionResourceMapper::toResource)
                .toList();
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutions result = {}", result);
        log.trace("getInstitutions end");

        return result;
    }

    @PostMapping(value = "/{institutionId}/products/{productId}/users", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "createInstitutionProductUser", description = "${swagger.dashboard.institutions.api.createInstitutionProductUser}", operationId = "#v2PostCreateInstitutionProductUser")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:CreateProductUsers')")
    public UserIdResource createInstitutionProductUser(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                                       @PathVariable("institutionId")
                                                       String institutionId,
                                                       @Parameter(description = "${swagger.dashboard.products.model.id}")
                                                       @PathVariable("productId")
                                                       String productId,
                                                       @RequestBody
                                                       @Valid
                                                       CreateUserDto user) {

        log.trace("createInstitutionProductUser start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "createInstitutionProductUser institutionId = {}, productId = {}, user = {}", institutionId, productId, user);
        String userId = userService.createUsers(institutionId, productId, userMapperV2.toUserToCreate(user));
        UserIdResource result = new UserIdResource();
        result.setId(UUID.fromString(userId));
        log.debug("createInstitutionProductUser result = {}", result);
        log.trace("createInstitutionProductUser end");
        return result;
    }

    @PutMapping(value = "/{institutionId}/products/{productId}/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "addUserProductRoles", description = "${swagger.dashboard.institutions.api.addUserProductRoles}", operationId = "#v2AddUserProductRole")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:CreateProductUsers')")
    public void addUserProductRoles(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                    @PathVariable("institutionId")
                                    String institutionId,
                                    @Parameter(description = "${swagger.dashboard.products.model.id}")
                                    @PathVariable("productId")
                                    String productId,
                                    @Parameter(description = "${swagger.dashboard.user.model.id}")
                                    @PathVariable("userId")
                                    String userId,
                                    @Parameter(description = "${swagger.dashboard.user.model.productRoles}")
                                    @RequestBody
                                    @Valid
                                    UserProductRoles userProductRoles) {
        log.trace("addUserProductRoles start");
        log.debug("institutionId = {}, productId = {}, userId = {}, userProductRoles = {}", Encode.forJava(institutionId),
                Encode.forJava(productId), Encode.forJava(userId), Encode.forJava(userProductRoles.toString()));
        userService.addUserProductRoles(institutionId, productId, userId, userProductRoles.getToAddOnAggregates(), userProductRoles.getProductRoles(), userProductRoles.getRole());
        log.trace("addUserProductRoles end");
    }

    @GetMapping(value = "/{institutionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.dashboard.institutions.api.getInstitution}", description = "${swagger.dashboard.institutions.api.getInstitution}", operationId = "#v2GetInstitution")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:ViewInstitutionData')")
    public InstitutionResource getInstitution(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                              @PathVariable("institutionId")
                                              String institutionId) {
        log.trace("getInstitution start");
        log.debug("getInstitution institutionId = {}", Encode.forJava(institutionId));

        Institution institution = institutionV2Service.findInstitutionById(institutionId);
        InstitutionResource result = institutionResourceMapper.toResource(institution);

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitution result = {}", result);
        log.trace("getInstitution end");
        return result;
    }

    @GetMapping(value = "/all/{institutionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.dashboard.institutions.api.getAllInstitution}", description = "${swagger.dashboard.institutions.api.getAllInstitution}", operationId = "#v2GetAllInstitution")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, null), 'Selc:ARB')")
    public InstitutionResource getAllInstitution(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                                 @PathVariable("institutionId") String institutionId) {
        log.trace("getAllInstitution start");
        log.debug("getAllInstitution institutionId = {}", Encode.forJava(institutionId));

        Institution institution = institutionV2Service.findAllInstitutionById(institutionId);
        InstitutionResource result = institutionResourceMapper.toResource(institution);

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getAllInstitution result = {}", result);
        log.trace("getAllInstitution end");
        return result;
    }

    @Operation(summary = "${swagger.dashboard.institutions.delegations}", description = "${swagger.dashboard.institutions.delegations}", operationId = "#getDelegationsUsingToUsingGET_1")
    @GetMapping(value = "/{institutionId}/institutions", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ViewDelegations')")
    public ResponseEntity<DelegationWithPagination> getDelegationsUsingTo(@Parameter(description = "${swagger.dashboard.delegation.model.to}")
                                                                          @PathVariable("institutionId") String institutionId,
                                                                          @Parameter(description = "${swagger.dashboard.delegation.model.productId}")
                                                                          @RequestParam(name = "productId", required = false) String productId,
                                                                          @Parameter(description = "${swagger.dashboard.delegation.model.description}")
                                                                          @RequestParam(name = "search", required = false) String search,
                                                                          @Parameter(description = "${swagger.dashboard.delegation.delegations.order}")
                                                                          @RequestParam(name = "order", required = false) Order order,
                                                                          @RequestParam(name = "page", required = false) @Min(0) Integer page,
                                                                          @RequestParam(name = "size", required = false) @Min(1) Integer size) {
        log.trace("getDelegationsUsingToV2 start");
        log.debug("getDelegationsUsingToV2 institutionId = {}, institutionDto{}", Encode.forJava(institutionId), Encode.forJava(productId));

        GetDelegationParameters delegationParameters = GetDelegationParameters.builder()
                .to(institutionId)
                .productId(productId)
                .search(search)
                .order(Objects.nonNull(order) ? order.name() : null)
                .page(page)
                .size(size)
                .build();

        ResponseEntity<DelegationWithPagination> result = ResponseEntity.status(HttpStatus.OK).body(delegationService.getDelegationsV2(delegationParameters));
        log.debug("getDelegationsUsingToV2 result = {}", result);
        log.trace("getDelegationsUsingToV2 end");
        return result;

    }

    @GetMapping(value = "/onboardings/{productId}/pending", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getInstitutionOnboardingPending", description = "${swagger.dashboard.institutions.api.getInstitutionOnboardingPending}", operationId = "#getInstitutionOnboardingPending")
    public ResponseEntity<Void> getInstitutionOnboardingPending(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                                                @RequestParam(name = "taxCode") String taxCode,
                                                                @RequestParam(name = "subunitCode", required = false) String subunitCode,
                                                                @PathVariable("productId")
                                                                @Parameter(description = "${swagger.dashboard.products.model.id}")
                                                                String productId) {

        log.trace("getInstitutionOnboardingPending start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitutionOnboardingPending productId = {}", productId);
        Boolean result = institutionV2Service.verifyIfExistsPendingOnboarding(taxCode, subunitCode, productId);
        if (Boolean.FALSE.equals(result)) {
            log.debug("Pending onboarding not found for and productId = {}", productId);
            log.trace("getInstitutionOnboardingPending end");
            return ResponseEntity.noContent().build();
        } else {
            log.debug("Pending onboarding found for and productId = {}", productId);
            log.trace("getInstitutionOnboardingPending end");
            return ResponseEntity.ok().build();
        }
    }

    @GetMapping(value = "/{institutionId}/products/{productId}/users/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getUserCount", description = "${swagger.dashboard.institutions.api.getUserCount}", operationId = "#v2GetUserCount")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:ListProductUsers')")
    public UserCountResource getUserCount(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                          @PathVariable("institutionId")
                                          String institutionId,
                                          @Parameter(description = "${swagger.dashboard.products.model.id}")
                                          @PathVariable("productId")
                                          String productId,
                                          @Parameter(description = "${swagger.dashboard.product-role-mappings.model.partyRoleList}")
                                          @RequestParam(name = "roles", required = false) String[] roles,
                                          @Parameter(description = "${swagger.dashboard.user.model.statusList}")
                                          @RequestParam(name = "status", required = false) String[] status) {
        log.trace("getUserCount start");
        UsersCountResponse userCount = userService.getUserCount(
                institutionId,
                productId,
                Optional.ofNullable(roles).map(Arrays::asList).orElse(Collections.emptyList()),
                Optional.ofNullable(status).map(Arrays::asList).orElse(Collections.emptyList())
        );
        UserCountResource result = userMapperV2.toUserCountResource(userCount);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getUserCount result = {}", result);
        log.trace("getUserCount end");
        return result;
    }

    @GetMapping(value = "/{institutionId}/onboardings-info", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getOnboardingsInfo", description = "${swagger.dashboard.institutions.api.getOnboardingsInfo}", operationId = "#v2GetOnboardingsInfo")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, null, null), 'Selc:ViewContract')")
    @ApiFeatureFlag("feature.viewcontract.enabled")
    public List<OnboardingInfo> getOnboardingsInfo(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                                   @PathVariable("institutionId") String institutionId,
                                                   @Parameter(description = "${swagger.dashboard.institutions.model.products}")
                                                   @RequestParam(name = "products", required = false) String[] products) {
        log.trace("getOnboardingsInfo start");
        OnboardingsResponse onboardingsResponse =
                institutionV2Service.getOnboardingsInfoResponse(
                        institutionId,
                        Optional.ofNullable(products).map(Arrays::asList).orElse(Collections.emptyList())
                );

        List<OnboardingInfo> result =
                onboardingsResponse.getOnboardings().stream()
                        .map(onboardingMapper::toOnboardingInfo)
                        .toList();

        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getOnboardingsInfo result = {}", result);
        log.trace("getOnboardingsInfo end");
        return result;
    }

    @GetMapping(value = "/{institutionId}/contract", produces = APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getContract", description = "${swagger.dashboard.institutions.api.getContract}", operationId = "#v2GetContract")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ViewContract')")
    @ApiFeatureFlag("feature.viewcontract.enabled")
    public ResponseEntity<byte[]> getContract(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                              @PathVariable("institutionId") String institutionId,
                                              @Parameter(description = "${swagger.dashboard.products.model.id}")
                                              @RequestParam(name = "productId") String productId) throws IOException {
        log.trace("getContract start");
        log.debug("getContract institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        Resource contract = institutionV2Service.getContract(institutionId, productId);
        return getResponseEntity(contract);
    }

    private ResponseEntity<byte[]> getResponseEntity(Resource contract) throws IOException {
        try (InputStream inputStream = contract.getInputStream()) {
            byte[] byteArray = IOUtils.toByteArray(inputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE);
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + contract.getFilename());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(byteArray);
        }
    }

    @PostMapping(value = "/{institutionId}/product/{productId}/check-user")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "checkUser", description = "${swagger.dashboard.institutions.api.checkUser}", operationId = "#v2CheckUser")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ListProductUsers')")
    public ResponseEntity<CheckUserResponse> checkUser(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                              @PathVariable("institutionId") String institutionId,
                                              @Parameter(description = "${swagger.dashboard.products.model.id}")
                                              @PathVariable(name = "productId") String productId,
                                              @Parameter(description = "${swagger.dashboard.user.model.productRoles}")
                                              @RequestBody
                                              @Valid
                                              SearchUserDto searchUserDto) {
        log.trace("checkUser start");
        log.debug("checkUser institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        Boolean isUserAlreadyOnboarded = userService.checkUser(searchUserDto.getFiscalCode(), institutionId, productId);
        log.trace("checkUser end");
        return ResponseEntity.ok().body(new CheckUserResponse(isUserAlreadyOnboarded));
    }

    @GetMapping(value = "/{institutionId}/product/{productId}/attachment/status")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "checkAttachmentStatus", description = "${swagger.dashboard.institutions.api.checkAttachmentStatus}", operationId = "#v2CheckAttachment")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(#institutionId, #productId, null), 'Selc:ViewContract')")
    public ResponseEntity<CheckAttachmentResponse> checkAttachmentStatus(@Parameter(description = "${swagger.dashboard.institutions.model.id}")
                                              @PathVariable("institutionId") String institutionId,
                                              @Parameter(description = "${swagger.dashboard.products.model.id}")
                                              @PathVariable(name = "productId") String productId,
                                              @RequestParam(name = "name") String name) {
        log.trace("checkAttachmentStatus start");
        log.debug("checkAttachmentStatus institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        Boolean response = institutionV2Service.checkAttachmentStatus(institutionId, productId, name);
        return ResponseEntity.ok().body(new CheckAttachmentResponse(response));
    }

}
