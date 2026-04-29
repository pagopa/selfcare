package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.api.DelegationConnector;
import it.pagopa.selfcare.mscore.constant.CustomError;
import it.pagopa.selfcare.mscore.constant.DelegationState;
import it.pagopa.selfcare.mscore.constant.DelegationType;
import it.pagopa.selfcare.mscore.constant.Order;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapper;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.exception.ResourceConflictException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.delegation.Delegation;
import it.pagopa.selfcare.mscore.model.delegation.DelegationInstitution;
import it.pagopa.selfcare.mscore.model.delegation.DelegationWithPagination;
import it.pagopa.selfcare.mscore.model.delegation.GetDelegationParameters;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.pagopa.selfcare.mscore.constant.CustomError.INSTITUTION_TAX_CODE_NOT_FOUND;
import static it.pagopa.selfcare.mscore.constant.GenericError.*;
@Slf4j
@Service
public class DelegationServiceImpl implements DelegationService {

    private static final int DEFAULT_DELEGATIONS_PAGE_SIZE = 10000;
    private final DelegationConnector delegationConnector;
    private final MailNotificationService notificationService;
    private final InstitutionService institutionService;
    private final InstitutionMapper institutionMapper;

    public DelegationServiceImpl(DelegationConnector delegationConnector,
                                 MailNotificationService notificationService,
                                 InstitutionService institutionService,
                                 InstitutionMapper institutionMapper) {
        this.delegationConnector = delegationConnector;
        this.notificationService = notificationService;
        this.institutionService = institutionService;
        this.institutionMapper = institutionMapper;
    }

    @Override
    public Delegation createDelegation(Delegation delegation) {

        setTaxCodesByInstitutionIds(delegation);

        Delegation savedDelegation = checkIfExistsAndSaveDelegation(delegation);

        if(delegation.getType().equals(DelegationType.PT)) {
            try {
                notificationService.sendMailForDelegation(delegation.getInstitutionFromName(), delegation.getProductId(), delegation.getTo());
            } catch (Exception e) {
                log.error(SEND_MAIL_FOR_DELEGATION_ERROR.getMessage() + ":", e.getMessage(), e);
            }
        }
        return savedDelegation;
    }

    private Delegation checkIfExistsAndSaveDelegation(Delegation delegation) {
        if(checkIfExistsWithStatus(delegation, DelegationState.ACTIVE)) {
            throw new ResourceConflictException(String.format(CustomError.CREATE_DELEGATION_CONFLICT.getMessage()),
                    CustomError.CREATE_DELEGATION_CONFLICT.getCode());
        }

        Delegation savedDelegation;
        try {
            if(checkIfExistsWithStatus(delegation, DelegationState.DELETED)){
                savedDelegation = delegationConnector.findAndActivate(delegation.getFrom(), delegation.getTo(), delegation.getProductId(), delegation.getIsTest());
            } else {
                delegation.setCreatedAt(OffsetDateTime.now());
                delegation.setStatus(DelegationState.ACTIVE);
                savedDelegation = delegationConnector.save(delegation);
            }
            institutionService.updateInstitutionDelegation(delegation.getTo(), true);
        } catch (Exception e) {
            throw new MsCoreException(CREATE_DELEGATION_ERROR.getMessage(), CREATE_DELEGATION_ERROR.getCode());
        }
        return savedDelegation;
    }

    private void setTaxCodesByInstitutionIds(Delegation delegation){
        /*
            Retrieve both delegator's and partner's institutions to set taxCodeFrom, fromType, taxCodeTo and toType
         */
        Institution institutionTo = institutionService.retrieveInstitutionById(delegation.getTo());
        Institution institutionFrom =institutionService.retrieveInstitutionById(delegation.getFrom());

        delegation.setToTaxCode(institutionTo.getTaxCode());
        delegation.setBrokerType(institutionMapper.getInstitutionType(institutionTo, delegation.getProductId()));

        delegation.setFromTaxCode(institutionFrom.getTaxCode());
        delegation.setInstitutionType(institutionMapper.getInstitutionType(institutionFrom, delegation.getProductId()));

        if (Boolean.TRUE.equals(institutionTo.getIsTest()) || Boolean.TRUE.equals(institutionFrom.getIsTest())) {
            delegation.setIsTest(true);
        }
    }

    @Override
    public Delegation createDelegationFromInstitutionsTaxCode(Delegation delegation) {

        List<Institution> institutionsTo = institutionService.getInstitutions(delegation.getToTaxCode(), delegation.getToSubunitCode());
        // TODO: remove filter when getInstitutions API will be fixed.
        /*
            If we call getInstitutions by empty subunitCode parameter, we have to filter retrieved list for institution
            with blank subunitCode field, otherwise we take first element returned by api.
        */
        Institution partner = institutionsTo.stream()
                .filter(institution -> StringUtils.hasText(delegation.getToSubunitCode()) || !StringUtils.hasText(institution.getSubunitCode()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTITUTION_TAX_CODE_NOT_FOUND.getMessage(), delegation.getToTaxCode()),
                        INSTITUTION_TAX_CODE_NOT_FOUND.getCode()));
        delegation.setTo(partner.getId());
        delegation.setBrokerType(institutionMapper.getInstitutionType(partner, delegation.getProductId()));

        // TODO: remove filter when getInstitutions API will be fixed.
        /*
            If we call getInstitutions by empty subunitCode parameter, we have to filter retrieved list for institution
            with blank subunitCode field, otherwise we take first element returned by api.
        */
        List<Institution> institutionsFrom = institutionService.getInstitutions(delegation.getFromTaxCode(), delegation.getFromSubunitCode());
        Institution from = institutionsFrom.stream()
                .filter(institution -> StringUtils.hasText(delegation.getFromSubunitCode()) || !StringUtils.hasText(institution.getSubunitCode()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(String.format(INSTITUTION_TAX_CODE_NOT_FOUND.getMessage(), delegation.getTo()),
                        INSTITUTION_TAX_CODE_NOT_FOUND.getCode()));
        delegation.setFrom(from.getId());
        delegation.setInstitutionType(institutionMapper.getInstitutionType(from, delegation.getProductId()));

        if (Boolean.TRUE.equals(partner.getIsTest()) || Boolean.TRUE.equals(from.getIsTest())) {
            delegation.setIsTest(true);
        }

        return checkIfExistsAndSaveDelegation(delegation);
    }

    @Override
    public void deleteDelegationByDelegationId(String delegationId) {
        String institutionId;
        try{
            Delegation delegation = delegationConnector.findByIdAndModifyStatus(delegationId, DelegationState.DELETED);
            institutionId = delegation.getTo();
        } catch (Exception e) {
            throw new MsCoreException(DELETE_DELEGATION_ERROR.getMessage(), DELETE_DELEGATION_ERROR.getCode());
        }
        try{
            if(!delegationConnector.checkIfDelegationsAreActive(institutionId)) {
                institutionService.updateInstitutionDelegation(institutionId, false);
            }
        } catch (Exception e) {
            delegationConnector.findByIdAndModifyStatus(delegationId, DelegationState.ACTIVE);
            throw new MsCoreException(DELETE_DELEGATION_ERROR.getMessage(), DELETE_DELEGATION_ERROR.getCode());
        }
    }

    @Override
    public boolean checkIfExistsWithStatus(Delegation delegation, DelegationState status) {
        return delegationConnector.checkIfExistsWithStatus(delegation, status);
    }

    @Override
    public List<Delegation> getDelegations(String from, String to, String productId, String search, String taxCode,
                                           Optional<Order> order, Optional<Integer> page, Optional<Integer> size) {
        int pageSize = size.filter(s -> s > 0).filter(s -> s <= DEFAULT_DELEGATIONS_PAGE_SIZE).orElse(DEFAULT_DELEGATIONS_PAGE_SIZE);
        return delegationConnector.find(from, to, productId, search, taxCode, order.orElse(Order.NONE), page.orElse(0), pageSize);
    }

    @Override
    public DelegationWithPagination getDelegationsV2(GetDelegationParameters delegationParameters) {
        return delegationConnector.findAndCount(delegationParameters);
    }

    @Override
    public List<DelegationInstitution> getDelegators(String institutionId, String productId, DelegationType type, Long cursor, int size) {
        final List<DelegationInstitution> delegators = delegationConnector.findDelegators(institutionId, productId, type, cursor, size);
        return removeOnboardingsFromDelegationInstitutions(delegators);
    }

    @Override
    public List<DelegationInstitution> getDelegates(String institutionId, String productId, DelegationType type, Long cursor, int size) {
        final List<DelegationInstitution> delegates = delegationConnector.findDelegates(institutionId, productId, type, cursor, size);
        return removeOnboardingsFromDelegationInstitutions(delegates);
    }

    /**
     * Return only the delegations with onboardings where productId == delegationProductId
     *
     * @param delegations list of delegators or delegates
     * @return list of valid delegations with only the relative onboarding
     */
    private List<DelegationInstitution> removeOnboardingsFromDelegationInstitutions(List<DelegationInstitution> delegations) {
        return delegations.stream().map(d -> {
            final String delegationProductId = Optional.ofNullable(d.getDelegationProductId()).orElse("");
            final List<Onboarding> filteredOnboardings = Optional.ofNullable(d.getInstitution().getOnboarding())
                    .map(lo -> lo.stream().filter(o -> delegationProductId.equals(o.getProductId())).toList())
                    .orElse(new ArrayList<>());
            d.getInstitution().setOnboarding(filteredOnboardings);
            return d;
        }).filter(d -> !d.getInstitution().getOnboarding().isEmpty()).toList();
    }

}
