package it.pagopa.selfcare.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.SelfCareUser;
import it.pagopa.selfcare.dashboard.model.backoffice.BrokerInfo;
import it.pagopa.selfcare.dashboard.model.mapper.BrokerResourceMapper;
import it.pagopa.selfcare.dashboard.model.mapper.ProductsMapper;
import it.pagopa.selfcare.dashboard.model.product.BrokerResource;
import it.pagopa.selfcare.dashboard.model.product.ProductRoleMappingsResource;
import it.pagopa.selfcare.dashboard.service.BrokerService;
import it.pagopa.selfcare.dashboard.service.ProductService;
import it.pagopa.selfcare.iam.generated.openapi.v1.dto.ProductRolePermissionsList;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/v1/products", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "products")
public class ProductController {

    private final ProductService productService;
    private final BrokerService brokerService;
    private final BrokerResourceMapper brokerResourceMapper;
    private static final String PAGO_PA_PRODUCT_ID = "prod-pagopa";

    @Autowired
    public ProductController(ProductService productService,
                             BrokerService brokerService,
                             BrokerResourceMapper brokerResourceMapper) {
        this.productService = productService;
        this.brokerService = brokerService;
        this.brokerResourceMapper = brokerResourceMapper;
    }

    @GetMapping(value = "/{productId}/roles", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getProductRoles", description = "${swagger.dashboard.product.api.getProductRoles}")
    public Collection<ProductRoleMappingsResource> getProductRoles(@Parameter(description = "${swagger.dashboard.products.model.id}")
                                                                   @PathVariable("productId") String productId,
                                                                  @Parameter(description = "${swagger.dashboard.institutions.model.institutionType}")
                                                                  @RequestParam(name = "institutionType", required = false) String institutionType) {
        log.trace("getProductRoles start");
        log.debug("productId = {}", Encode.forJava(productId));
        Collection<ProductRoleMappingsResource> result = ProductsMapper.toProductRoleMappingsResource(productService.getProductRoles(productId, institutionType));
        log.debug("getProductRoles result = {}", result);
        log.trace("getProductRoles end");

        return result;
    }

    @GetMapping(value = "/{productId}/brokers/{institutionType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "getProductBrokers", description = "${swagger.dashboard.product.api.getProductBrokers}")
    public Collection<BrokerResource> getProductBrokers(@Parameter(description = "${swagger.dashboard.products.model.id}")
                                                        @PathVariable("productId")
                                                        String productId,
                                                        @Parameter(description = "${swagger.dashboard.products.model.institutionType}")
                                                        @PathVariable("institutionType")
                                                        String institutionType) {
        log.trace("getProductBrokers start");
        log.debug("productId = {}, institutionType = {}", Encode.forJava(productId), Encode.forJava(institutionType));

        List<BrokerInfo> brokers = PAGO_PA_PRODUCT_ID.equals(productId)
            ?  brokerService.findAllByInstitutionType(InstitutionType.valueOf(institutionType).name())
            : brokerService.findInstitutionsByProductAndType(productId, institutionType);

        Collection<BrokerResource>  result = brokerResourceMapper.toResourceList(brokers);
        log.debug("getProductBrokers result = {}", result);
        log.trace("getProductBrokers end");
        return result;
    }

    @GetMapping(value = "/my-permissions", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "${swagger.dashboard.product.api.getMyPermissions}", description = "${swagger.dashboard.product.api.getMyPermissions}", operationId = "#getMyPermissions")
    @PreAuthorize("hasPermission(new it.pagopa.selfcare.dashboard.security.FilterAuthorityDomain(null, null, null), 'Selc:ARB')")
    public ProductRolePermissionsList getMyPermissions(Authentication authentication) {
        log.trace("getMyPermissions start");
        String loggedUserId = ((SelfCareUser) authentication.getPrincipal()).getId();
        ProductRolePermissionsList result = productService.getMyPermissions(loggedUserId);
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getMyPermissions result = {}", result);
        log.trace("getMyPermissions end");
        return result;
    }


}
