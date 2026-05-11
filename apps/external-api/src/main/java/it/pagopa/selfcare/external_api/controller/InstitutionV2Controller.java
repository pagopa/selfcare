package it.pagopa.selfcare.external_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.external_api.mapper.InstitutionResourceMapper;
import it.pagopa.selfcare.external_api.mapper.UserInfoResourceMapper;
import it.pagopa.selfcare.external_api.model.document.ResourceResponse;
import it.pagopa.selfcare.external_api.model.institution.InstitutionResource;
import it.pagopa.selfcare.external_api.model.product.ProductResource;
import it.pagopa.selfcare.external_api.model.user.UserProductResponse;
import it.pagopa.selfcare.external_api.model.user.UserResource;
import it.pagopa.selfcare.external_api.service.ContractService;
import it.pagopa.selfcare.external_api.service.InstitutionService;
import it.pagopa.selfcare.external_api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/v2/institutions", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Institution")
@RequiredArgsConstructor
public class InstitutionV2Controller {

    private final ContractService contractService;
    private final InstitutionService institutionService;
    private final UserService userService;
    private final InstitutionResourceMapper institutionResourceMapper;
    private final UserInfoResourceMapper userInfoResourceMapper;

    @Tag(name = "Institution")
    @Tag(name = "external-pnpg")
    @GetMapping(value = "")
    @ResponseStatus(HttpStatus.OK)
    @Deprecated
    @Operation(summary = "getInstitutions", description = "${swagger.external_api.institutions.api.getInstitutions}", operationId = "#getInstitutionsUsingGETDeprecated")
    public List<InstitutionResource> getInstitutions(@Parameter(description = "${swagger.external_api.products.model.id}")
                                                     @RequestParam(value = "productId") String productId,
                                                     Authentication authentication) {
        log.trace("getInstitutions start");
        SelfCareUser user = (SelfCareUser) authentication.getPrincipal();
        List<InstitutionResource> institutionResources = userService.getOnboardedInstitutionsDetailsActive(user.getId(), productId)
                .stream()
                .map(institutionResourceMapper::toResource)
                .toList();
        log.debug("getInstitutions result = {}", institutionResources);
        log.trace("getInstitutions end");
        return institutionResources;
    }

    @Tag(name = "external-v2")
    @Tag(name = "support")
    @Tag(name = "contract")
    @Tag(name = "Institution")
    @GetMapping(value = "/{institutionId}/contract", produces = APPLICATION_OCTET_STREAM_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getContract", description = "${swagger.external_api.documents.api.getContract}")
    public ResponseEntity<byte[]> getContract(@Parameter(description = "${swagger.external_api.institutions.model.id}")
                                              @PathVariable("institutionId") String institutionId,
                                              @Parameter(description = "${swagger.external_api.products.model.id}")
                                              @RequestParam(value = "productId", required = false) String productId){
        log.trace("getContract start");
        log.debug("getContract institutionId = {}, productId = {}", Encode.forJava(institutionId), Encode.forJava(productId));
        ResourceResponse contract = contractService.getContractV2(institutionId, productId);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, APPLICATION_OCTET_STREAM_VALUE);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + contract.getFileName());
        log.info("contentType: {}", headers.getContentType());
        log.debug("getContract result = {}", contract);
        log.trace("getContract end");
        return ResponseEntity.ok().headers(headers).body(contract.getData());
    }

    @Tag(name = "external-v2")
    @Tag(name = "external-pnpg")
    @Tag(name = "Institution")
    @GetMapping(value = "/{institutionId}/products")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getInstitutionProducts", description = "${swagger.external_api.institutions.api.getInstitutionUserProducts}")
    public List<ProductResource> getInstitutionProducts(@Parameter(description = "${swagger.external_api.institutions.model.id}")
                                                        @PathVariable("institutionId") String institutionId,
                                                        @Parameter(description = "${swagger.external_api.user.model.id}")
                                                        @RequestParam(value = "userId") String userId) {
        log.trace("getInstitutionUserProducts start");
        List<ProductResource> productResources = institutionService.getInstitutionUserProductsV2(institutionId, userId);
        log.debug("getInstitutionUserProducts result = {}", productResources);
        log.trace("getInstitutionUserProducts end");
        return productResources;
    }

    @Tag(name = "external-v2")
    @Tag(name = "internal-v1")
    @Tag(name = "external-pnpg")
    @Tag(name = "Institution")
    @GetMapping(value = "/{institutionId}/users")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getInstitutionUsersByProduct", description = "${swagger.external_api.institutions.api.getInstitutionProductUsers}")
    public List<UserResource> getInstitutionUsersByProduct(@Parameter(description = "${swagger.external_api.institutions.model.id}")
                                                           @PathVariable("institutionId") String institutionId,
                                                           @Parameter(description = "${swagger.external_api.products.model.id}")
                                                           @RequestParam(value = "productId", required = false) // Even if productId is required by the service, we leave it optional: the external policy should enforce it
                                                           String productId,
                                                           @Parameter(description = "${swagger.external_api.user.model.id}")
                                                           @RequestParam(value = "userId", required = false)
                                                           Optional<String> userId,
                                                           @Parameter(description = "${swagger.external_api.model.productRoles}")
                                                           @RequestParam(value = "productRoles", required = false)
                                                           Optional<Set<String>> productRoles,
                                                           @RequestHeader(value = "x-selfcare-uid", required = false) Optional<String> xSelfCareUid) {
        Collection<UserProductResponse> userInfos = institutionService.getInstitutionProductUsersV2(institutionId, productId, userId.orElse(null), productRoles.orElse(null), xSelfCareUid.orElse(null));
        List<UserResource> result = userInfos.stream().map(userInfoResourceMapper::toUserResource).toList();
        log.debug("getInstitutionProductUsers result = {}", result);
        log.trace("getInstitutionProductUsers end");
        return result;
    }
}
