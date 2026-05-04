package it.pagopa.selfcare.mscore.core.strategy;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.exception.ResourceNotFoundException;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.NationalRegistriesProfessionalAddress;
import it.pagopa.selfcare.mscore.utils.MaskDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.selfcare.mscore.constant.GenericError.CREATE_INSTITUTION_ERROR;
import static it.pagopa.selfcare.mscore.constant.GenericError.INSTITUTION_INFOCAMERE_NOTFOUND;

@Slf4j
@Component
public class CreateInstitutionStrategyInfocamere extends CreateInstitutionStrategyCommon implements CreateInstitutionStrategy {

    private final PartyRegistryProxyConnector partyRegistryProxyConnector;

    private Institution institution;

    public CreateInstitutionStrategyInfocamere(PartyRegistryProxyConnector partyRegistryProxyConnector,
                                               InstitutionConnector institutionConnector) {
        super(institutionConnector);
        this.partyRegistryProxyConnector = partyRegistryProxyConnector;
    }


    @Override
    public Institution createInstitution(CreateInstitutionStrategyInput strategyInput) {
        Institution toSavedOrUpdate;
        List<Institution> institutions = institutionConnector.findByTaxCodeAndSubunitCode(strategyInput.getTaxCode(), null, null);
        if (institutions.isEmpty()) {
            //Institution does not exist, it will be created
            try {
                NationalRegistriesProfessionalAddress professionalAddress = partyRegistryProxyConnector.getLegalAddress(strategyInput.getTaxCode());
                fillInstitutionFromInfocamereData(strategyInput.getTaxCode(), strategyInput.getDescription(), professionalAddress);
            } catch (MsCoreException ex) {
                if (ex.getCode().equalsIgnoreCase(String.valueOf(HttpStatus.NOT_FOUND.value()))) {
                    log.warn(String.format(INSTITUTION_INFOCAMERE_NOTFOUND.getMessage(), Encode.forJava(MaskDataUtils.maskString(strategyInput.getTaxCode()))));
                    fillInstitutionRawData(strategyInput);
                } else {
                    throw ex;
                }
            } catch (ResourceNotFoundException ex) {
                log.warn(String.format(INSTITUTION_INFOCAMERE_NOTFOUND.getMessage(), Encode.forJava(MaskDataUtils.maskString(strategyInput.getTaxCode()))));
                fillInstitutionRawData(strategyInput);
            }

            toSavedOrUpdate = institution;
        } else {
            //Institution exists but description could be updated
            toSavedOrUpdate = institutions.get(0);
            toSavedOrUpdate.setDescription(strategyInput.getDescription());
            toSavedOrUpdate.setUpdatedAt(OffsetDateTime.now());
        }

        setIstatCode(strategyInput, toSavedOrUpdate);

        try {
            return institutionConnector.save(toSavedOrUpdate);
        } catch (Exception e) {
            throw new MsCoreException(CREATE_INSTITUTION_ERROR.getMessage(), CREATE_INSTITUTION_ERROR.getCode());
        }
    }

    private void fillInstitutionRawData(CreateInstitutionStrategyInput strategyInput) {
        institution.setExternalId(getExternalId(strategyInput));
        institution.setOrigin(Origin.ADE.getValue());
        institution.setOriginId(strategyInput.getTaxCode());
        institution.setCreatedAt(OffsetDateTime.now());
    }

    private void fillInstitutionFromInfocamereData(String taxCode, String description, NationalRegistriesProfessionalAddress professionalAddress) {
        institution.setAddress(professionalAddress.getAddress());
        institution.setZipCode(professionalAddress.getZipCode());
        institution.setTaxCode(taxCode);
        institution.setDescription(description);
        institution.setExternalId(taxCode);
        institution.setOrigin(Origin.INFOCAMERE.getValue());
        institution.setOriginId(taxCode);
        institution.setCreatedAt(OffsetDateTime.now());
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }
}
