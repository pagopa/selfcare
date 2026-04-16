package it.pagopa.selfcare.mscore.core.strategy;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.mscore.constant.CustomError;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapper;
import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.institution.*;
import it.pagopa.selfcare.mscore.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.selfcare.mscore.constant.GenericError.CREATE_INSTITUTION_ERROR;

@Slf4j
@Component
public class CreateInstitutionStrategyPda extends CreateInstitutionStrategyCommon implements CreateInstitutionStrategy {

    private final PartyRegistryProxyConnector partyRegistryProxyConnector;

    private final InstitutionMapper institutionMapper;

    private String injectionInstitutionType;

    public CreateInstitutionStrategyPda(PartyRegistryProxyConnector partyRegistryProxyConnector,
                                        InstitutionConnector institutionConnector,
                                        InstitutionMapper institutionMapper) {
        super(institutionConnector);
        this.partyRegistryProxyConnector = partyRegistryProxyConnector;
        this.institutionMapper = institutionMapper;
    }


    @Override
    public Institution createInstitution(CreateInstitutionStrategyInput strategyInput) {
        checkIfAlreadyExistsByTaxCodeAndSubunitCode(strategyInput.getTaxCode(), strategyInput.getSubunitCode());

        Institution institution;
        try {
            institution = searchInstitutionOnIpa(strategyInput.getTaxCode());
        } catch (ResourceNotFoundException ex) {
            institution = searchInstitutionOnInfocamere(strategyInput);
        }

        try {
            return institutionConnector.save(institution);
        } catch (Exception e) {
            throw new MsCoreException(CREATE_INSTITUTION_ERROR.getMessage(), CREATE_INSTITUTION_ERROR.getCode());
        }
    }

    private Institution searchInstitutionOnIpa(String taxCode) {
        InstitutionProxyInfo institutionProxyInfo;
        Institution institution = null;
        try {
            institutionProxyInfo = partyRegistryProxyConnector.getInstitutionById(taxCode);
            if (institutionProxyInfo != null) {
                final CategoryProxyInfo categoryProxyInfo = partyRegistryProxyConnector.getCategory(institutionProxyInfo.getOrigin(), institutionProxyInfo.getCategory());
                institution = mapInstitutionFromIpa(taxCode, institutionProxyInfo, categoryProxyInfo);
            }
        } catch (MsCoreException ex) {
            if(ex.getCode().equalsIgnoreCase(String.valueOf(HttpStatus.NOT_FOUND.value()))) {
                log.debug("Institution with taxCode: {} not found in IPA", MaskDataUtils.maskString(taxCode));
                throw new ResourceNotFoundException(CustomError.INSTITUTION_NOT_FOUND_IN_REGISTRY.getMessage(), CustomError.INSTITUTION_NOT_FOUND_IN_REGISTRY.getCode());
            }

            throw ex;
        } catch (ResourceNotFoundException ex) {
            log.debug("Institution with taxCode: {} not found in IPA", MaskDataUtils.maskString(taxCode));
            throw ex;
        }

        return institution;
    }

    private Institution searchInstitutionOnInfocamere(CreateInstitutionStrategyInput strategyInput){
        try {
            NationalRegistriesProfessionalAddress professionalAddress = partyRegistryProxyConnector.getLegalAddress(strategyInput.getTaxCode());
            return getInstitutionFromInfocamere(strategyInput.getTaxCode(), strategyInput.getDescription(), strategyInput.getIstatCode(), professionalAddress);
        } catch (MsCoreException ex) {
            if(ex.getCode().equalsIgnoreCase(String.valueOf(HttpStatus.NOT_FOUND.value()))) {
                log.warn("Institution with taxCode {} not found in registry INFOCAMERE", MaskDataUtils.maskString(strategyInput.getTaxCode()));
                throw new ResourceNotFoundException(String.format(CustomError.INSTITUTION_NOT_FOUND_IN_REGISTRY.getMessage(), strategyInput.getDescription()), CustomError.INSTITUTION_NOT_FOUND_IN_REGISTRY.getCode());
            }
            throw ex;
        }
    }

    private Institution getInstitutionFromInfocamere(String taxCode, String description, String istatCode, NationalRegistriesProfessionalAddress professionalAddress) {
        Institution newInstitution = institutionMapper.fromProfessionalAddress(professionalAddress);
        newInstitution.setTaxCode(taxCode);
        newInstitution.setDescription(description);
        newInstitution.setExternalId(taxCode);
        newInstitution.setOrigin(Origin.INFOCAMERE.getValue());
        newInstitution.setIstatCode(istatCode);
        newInstitution.setOriginId(taxCode);
        newInstitution.setCreatedAt(OffsetDateTime.now());
        newInstitution.setImported(true);

        return newInstitution;
    }

    private Institution mapInstitutionFromIpa(String taxCode, InstitutionProxyInfo institutionProxyInfo, CategoryProxyInfo categoryProxyInfo) {

        Institution newInstitution = institutionMapper.fromInstitutionProxyInfo(institutionProxyInfo);
        newInstitution.setExternalId(taxCode);
        newInstitution.setOrigin(Origin.IPA.getValue());
        newInstitution.setCreatedAt(OffsetDateTime.now());
        newInstitution.setImported(true);

        Attributes attributes = new Attributes();
        attributes.setOrigin(categoryProxyInfo.getOrigin());
        attributes.setCode(categoryProxyInfo.getCode());
        attributes.setDescription(categoryProxyInfo.getName());
        newInstitution.setAttributes(List.of(attributes));

        return newInstitution;
    }

    public void setInjectionInstitutionType(String injectionInstitutionType) {
        this.injectionInstitutionType = injectionInstitutionType;
    }
}
