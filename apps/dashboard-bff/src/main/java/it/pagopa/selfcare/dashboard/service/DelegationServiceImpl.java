package it.pagopa.selfcare.dashboard.service;

import it.pagopa.selfcare.commons.base.logging.LogUtils;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.DelegationResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.DelegationWithPaginationResponse;
import it.pagopa.selfcare.core.generated.openapi.v1.dto.InstitutionsResponse;
import it.pagopa.selfcare.dashboard.client.CoreDelegationApiRestClient;
import it.pagopa.selfcare.dashboard.client.CoreInstitutionApiRestClient;
import it.pagopa.selfcare.dashboard.exception.ResourceNotFoundException;
import it.pagopa.selfcare.dashboard.model.delegation.*;
import it.pagopa.selfcare.dashboard.model.institution.Institution;
import it.pagopa.selfcare.dashboard.model.institution.RelationshipState;
import it.pagopa.selfcare.dashboard.model.mapper.DelegationRestClientMapper;
import it.pagopa.selfcare.dashboard.model.mapper.InstitutionMapper;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;

@Slf4j
@Service
public class DelegationServiceImpl implements DelegationService {

    private final CoreInstitutionApiRestClient coreInstitutionApiRestClient;
    private final CoreDelegationApiRestClient coreDelegationApiRestClient;

    private final DelegationRestClientMapper delegationMapper;
    private final InstitutionMapper institutionMapper;

    private static final String INSTITUTION_TAX_CODE_NOT_FOUND = "Cannot find Institution using taxCode %s";
    static final String REQUIRED_TAX_CODE_MESSAGE = "A taxCode is required";

    @Autowired
    public DelegationServiceImpl(CoreInstitutionApiRestClient coreInstitutionApiRestClient,
                                 CoreDelegationApiRestClient coreDelegationApiRestClient,
                                 DelegationRestClientMapper delegationMapper,
                                 InstitutionMapper institutionMapper) {
        this.coreInstitutionApiRestClient = coreInstitutionApiRestClient;
        this.coreDelegationApiRestClient = coreDelegationApiRestClient;
        this.delegationMapper = delegationMapper;
        this.institutionMapper = institutionMapper;
    }

    @Override
    public DelegationId createDelegation(DelegationRequest delegation) {
        log.trace("createDelegation start");
        log.debug(LogUtils.CONFIDENTIAL_MARKER, "createDelegation request = {}", delegation);

        /*
            In case of prod-pagopa product, in the attribute "to" of the delegation object a taxCode is inserted.
            So we have to retrieve the institutionId from the taxCode and set it in the "to" attribute.
         */
        if (PROD_PAGOPA.getValue().equals(delegation.getProductId())) {
            setToInstitutionId(delegation);
        }
        DelegationId delegationId = new DelegationId();
        DelegationResponse result = coreDelegationApiRestClient._createDelegationUsingPOST(delegationMapper.toDelegationRequest(delegation)).getBody();
        log.debug("createDelegation result = {}", result);
        log.trace("createDelegation end");
        if (result != null) {
            delegationId.setId(result.getId());
        }
        return delegationId;
    }

    void setToInstitutionId(DelegationRequest delegation) {
        log.trace("getInstitution start");
        log.debug("getInstitution institutionId = {}", Encode.forJava(delegation.getTo()));
        Assert.hasText(delegation.getTo(), REQUIRED_TAX_CODE_MESSAGE);

        InstitutionsResponse institutionsResponse =
                coreInstitutionApiRestClient._getInstitutionsUsingGET(delegation.getTo(), null, null, null, null, null).getBody();

        if (institutionsResponse != null) {
            List<Institution> institutions = institutionsResponse.getInstitutions()
                    .stream()
                    .map(institutionMapper::toInstitution)
                    .toList();

            Institution partner = institutions.stream()
                    .filter(institution -> institution.getOnboarding() != null &&
                            institution.getOnboarding().stream()
                                    .anyMatch(onb ->
                                            delegation.getProductId().equals(onb.getProductId())
                                                    && RelationshipState.ACTIVE.equals(onb.getStatus())
                                    )
                    // Prefer institution with onboarding of type PT
                    // Keep in mind that false < true in boolean comparison
                    ).min(Comparator.comparing(inst -> inst.getOnboarding().stream()
                            .noneMatch(onb ->
                                    delegation.getProductId().equals(onb.getProductId()) &&
                                    RelationshipState.ACTIVE.equals(onb.getStatus()) &&
                                    InstitutionType.PT.equals(onb.getInstitutionType())
                            )
                    ))
                    .orElseThrow(() -> {
                        log.warn("(setToInstitutionId) Cannot find Institution for taxCode {}", Encode.forJava(delegation.getTo()));
                        return new ResourceNotFoundException(String.format(INSTITUTION_TAX_CODE_NOT_FOUND, delegation.getTo()));
                    });

            if (institutions.size() > 1) {
                log.warn("(setToInstitutionId) Multiple institutions found for taxCode {}: selected institutionId {}",
                        Encode.forJava(delegation.getTo()), Encode.forJava(partner.getId()));
            }
            delegation.setTo(partner.getId());
        }
    }

    @Override
    public List<Delegation> getDelegations(GetDelegationParameters delegationParameters) {
        log.trace("getDelegations start");
        log.debug("getDelegations productId = {}, type = {}", Encode.forJava(delegationParameters.getFrom()), Encode.forJava(delegationParameters.getProductId()));
        List<DelegationResponse> delegationsResponse = coreDelegationApiRestClient._getDelegationsUsingGET(
                        delegationParameters.getFrom(),
                        delegationParameters.getTo(),
                        delegationParameters.getProductId(),
                        delegationParameters.getSearch(),
                        delegationParameters.getTaxCode(),
                        delegationParameters.getOrder(),
                        delegationParameters.getPage(),
                        delegationParameters.getSize())
                .getBody();

        if (Objects.isNull(delegationsResponse))
            return List.of();

        List<Delegation> delegations = delegationsResponse.stream()
                .map(delegationMapper::toDelegations)
                .toList();
        log.debug("getDelegations result = {}", delegations);
        log.trace("getDelegations end");
        return delegations;
    }

    @Override
    public DelegationWithPagination getDelegationsV2(GetDelegationParameters delegationParameters) {
        log.trace("getDelegationsV2 start");
        log.debug("getDelegationsV2 productId = {}, type = {}", Encode.forJava(delegationParameters.getFrom()), Encode.forJava(delegationParameters.getProductId()));
        DelegationWithPaginationResponse delegationsResponse = coreDelegationApiRestClient._getDelegationsUsingGET1(
                        delegationParameters.getFrom(),
                        delegationParameters.getTo(),
                        delegationParameters.getProductId(),
                        delegationParameters.getSearch(),
                        delegationParameters.getTaxCode(),
                        delegationParameters.getOrder(),
                        delegationParameters.getPage(),
                        delegationParameters.getSize())
                .getBody();

        assert delegationsResponse != null;

        List<DelegationWithInfo> delegations = delegationsResponse.getDelegations().stream()
                .map(delegationMapper::toDelegationsWithInfo)
                .toList();
        PageInfo pageInfo = delegationMapper.toPageInfo(delegationsResponse.getPageInfo());
        DelegationWithPagination delegationWithPagination = new DelegationWithPagination(delegations, pageInfo);
        log.debug("getDelegationsV2 result = {}", delegations);
        log.trace("getDelegationsV2 end");
        return delegationWithPagination;
    }


}