package it.pagopa.selfcare.mscore.core.strategy;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.SaResource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.selfcare.mscore.constant.GenericError.CREATE_INSTITUTION_ERROR;

@Slf4j
public class CreateInstitutionStrategyAnac extends CreateInstitutionStrategyCommon implements CreateInstitutionStrategy {
    private final PartyRegistryProxyConnector partyRegistryProxyConnector;

    @Setter
    private Institution institution;

    public CreateInstitutionStrategyAnac(PartyRegistryProxyConnector partyRegistryProxyConnector,
                                         InstitutionConnector institutionConnector) {
        super(institutionConnector);
        this.partyRegistryProxyConnector = partyRegistryProxyConnector;
    }

    @Override
    public Institution createInstitution(CreateInstitutionStrategyInput strategyInput) {

        List<Institution> institutions = institutionConnector.findByTaxCodeAndSubunitCode(strategyInput.getTaxCode(), strategyInput.getSubunitCode(), null);

        if (institutions.isEmpty()) {

            SaResource saResource = partyRegistryProxyConnector.getSAFromAnac(strategyInput.getTaxCode());

            institution = addFieldsToInstitution(saResource);

            setContacts(strategyInput, institution);

            setIstatCode(strategyInput, institution);

        } else {
            //Institution exists but other fields could be updated
            institution = institutions.get(0);
            setUpdatedFields(strategyInput, institution);
        }

        try {
            return institutionConnector.save(institution);
        } catch (Exception e) {
            throw new MsCoreException(CREATE_INSTITUTION_ERROR.getMessage(), CREATE_INSTITUTION_ERROR.getCode());
        }
    }

    private Institution addFieldsToInstitution(SaResource saResource) {

        institution.setExternalId(institution.getTaxCode());
        institution.setOrigin(Origin.ANAC.getValue());
        institution.setOriginId(saResource.getTaxCode());
        institution.setCreatedAt(OffsetDateTime.now());
        institution.setDigitalAddress(saResource.getDigitalAddress());
        institution.setDescription(saResource.getDescription());

        return institution;
    }

}
