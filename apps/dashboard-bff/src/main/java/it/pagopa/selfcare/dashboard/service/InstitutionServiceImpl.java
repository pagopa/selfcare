package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.commons.base.security.ProductGrantedAuthority;
import it.pagopa.selfcare.commons.base.security.SelfCareGrantedAuthority;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionPut;
import it.pagopa.selfcare.dashboard.client.CoreInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.model.institution.GeographicTaxonomy;
import it.pagopa.selfcare.dashboard.model.institution.GeographicTaxonomyList;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.institution.UpdateInstitutionResource;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionMapper;
import it.pagopa.selfcare.dashboard.model.product.ProductTree;
import it.pagopa.selfcare.dashboard.model.product.mapper.ProductMapper;
import it.pagopa.selfcare.product.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static it.pagopa.selfcare.commons.base.security.SelfCareAuthority.LIMITED;

@Slf4j
@Service
public class InstitutionServiceImpl implements InstitutionService {

    private final CoreInstitutionApiRestClient coreInstitutionApiRestClient;
    private final ProductService productService;

    private final InstitutionMapper institutionMapper;
    private final ProductMapper productMapper;

    static final String REQUIRED_INSTITUTION_MESSAGE = "An Institution id is required";
    static final String REQUIRED_GEOGRAPHIC_TAXONOMIES = "An object of geographic taxonomy list is required";
    static final String REQUIRED_UPDATE_RESOURCE_MESSAGE = "An Institution update resource is required";

    @Autowired
    public InstitutionServiceImpl(CoreInstitutionApiRestClient coreInstitutionApiRestClient,
                                  ProductService productService,
                                  InstitutionMapper institutionMapper,
                                  ProductMapper productMapper) {
        this.coreInstitutionApiRestClient = coreInstitutionApiRestClient;
        this.productService = productService;
        this.institutionMapper = institutionMapper;
        this.productMapper = productMapper;
    }


    @Override
    public Institution getInstitutionById(String institutionId) {
        log.trace("getInstitution start");
        Institution result = institutionMapper.toInstitution(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null).getBody());
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "getInstitution result = {}", result);
        log.trace("getInstitution end");
        return result;
    }

    @Override
    public void updateInstitutionGeographicTaxonomy(String institutionId, GeographicTaxonomyList geographicTaxonomies) {
        log.trace("updateInstitutionGeographicTaxonomy start");
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_MESSAGE);
        Assert.notNull(geographicTaxonomies, REQUIRED_GEOGRAPHIC_TAXONOMIES);
        log.debug("updateInstitutionGeographicTaxonomy institutionId = {}, geograpihc taxonomies = {}", Encode.forJava(institutionId), Encode.forJava(geographicTaxonomies.toString()));
        InstitutionPut geographicTaxonomiesRequest = new InstitutionPut();
        geographicTaxonomiesRequest.setGeographicTaxonomyCodes(geographicTaxonomies.getGeographicTaxonomyList().stream().map(GeographicTaxonomy::getCode).toList());
        coreInstitutionApiRestClient._updateInstitutionUsingPUT(institutionId, geographicTaxonomiesRequest);
        log.trace("updateInstitutionGeographicTaxonomy end");
    }

    @Override
    public List<ProductTree> getProductsTree() {
        log.trace("getProductsTree start");
        List<ProductTree> productTrees = productMapper.toTreeResource(productService.getProducts(false, true));
        log.debug("getInstitutionProducts result = {}", productTrees);
        log.trace("getInstitutionProducts end");
        return productTrees;
    }

    @Override
    public Institution updateInstitutionDescription(String institutionId, UpdateInstitutionResource updateInstitutionResource) {
        log.trace("updateInstitutionDescription start");
        log.debug("updateInstitutionDescription institutionId = {}, updateInstitutionResource = {}", Encode.forJava(institutionId), updateInstitutionResource);
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_MESSAGE);
        Assert.notNull(updateInstitutionResource, REQUIRED_UPDATE_RESOURCE_MESSAGE);
        Institution institution = institutionMapper.toInstitution(coreInstitutionApiRestClient._updateInstitutionUsingPUT(institutionId, institutionMapper.toInstitutionPut(updateInstitutionResource)).getBody());
        log.debug("updateInstitutionDescription result = {}", institution);
        log.trace("updateInstitutionDescription end");
        return institution;
    }

    @Override
    public Institution findInstitutionById(String institutionId) {
        log.trace("findInstitutionById start");
        log.debug("findInstitutionById institutionId = {}", Encode.forJava(institutionId));
        Assert.hasText(institutionId, REQUIRED_INSTITUTION_MESSAGE);
        Institution institution = institutionMapper.toInstitution(coreInstitutionApiRestClient._retrieveInstitutionByIdUsingGET(institutionId, null).getBody());
        if (institution != null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Optional<? extends GrantedAuthority> selcAuthority = authentication.getAuthorities()
                    .stream()
                    .filter(grantedAuthority -> SelfCareGrantedAuthority.class.isAssignableFrom(grantedAuthority.getClass()))
                    .map(SelfCareGrantedAuthority.class::cast)
                    .filter(grantedAuthority -> institutionId.equals(grantedAuthority.getInstitutionId()))
                    .findAny();

            if (selcAuthority.isPresent()) {
                Map<String, ProductGrantedAuthority> userAuthProducts = ((SelfCareGrantedAuthority) selcAuthority.get()).getRoleOnProducts();

                if (LIMITED.name().equals(selcAuthority.get().getAuthority())) {
                    institution.setOnboarding(institution.getOnboarding().stream()
                            .filter(product -> userAuthProducts.containsKey(product.getProductId()))
                            .peek(product -> product.setUserRole(LIMITED.name()))
                            .toList());
                } else {
                    institution.getOnboarding().forEach(product -> Optional.ofNullable(userAuthProducts.get(product.getProductId()))
                            .ifPresentOrElse(authority -> product.setUserRole(authority.getAuthority()), () -> product.setUserRole(null)));
                }
            }
        }
        log.debug("findInstitutionById result = {}", institution);
        log.trace("findInstitutionById end");
        return institution;
    }

}
