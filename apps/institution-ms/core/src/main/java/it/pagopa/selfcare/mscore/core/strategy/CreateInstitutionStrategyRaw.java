package it.pagopa.selfcare.mscore.core.strategy;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.constant.Origin;
import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.exception.MsCoreException;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static it.pagopa.selfcare.mscore.constant.GenericError.CREATE_INSTITUTION_ERROR;

@Component
public class CreateInstitutionStrategyRaw extends CreateInstitutionStrategyCommon implements CreateInstitutionStrategy {

    private Institution institution;

    public CreateInstitutionStrategyRaw(InstitutionConnector institutionConnector) {
        super(institutionConnector);
    }

    @Override
    public Institution createInstitution(CreateInstitutionStrategyInput strategyInput) {

        List<Institution> institutions = institutionConnector.findByTaxCodeAndSubunitCode(strategyInput.getTaxCode(), strategyInput.getSubunitCode(), null);

        if (institutions.isEmpty()) {
            institution.setExternalId(getExternalId(strategyInput));
            if(!StringUtils.hasText(institution.getOrigin())){
                institution.setOrigin(Origin.SELC.getValue());
            }
            if(!StringUtils.hasText(institution.getOriginId())){
                institution.setOriginId("SELC_" + institution.getExternalId());
            }
            institution.setCreatedAt(OffsetDateTime.now());
            setContacts(strategyInput, institution);
            setIstatCode(strategyInput, institution);
            setLegalForm(strategyInput, institution);
        } else {
            institution = institutions.get(0);
            setUpdatedFields(strategyInput, institution);
        }

        try {
            return institutionConnector.save(institution);
        } catch (Exception e) {
            throw new MsCoreException(CREATE_INSTITUTION_ERROR.getMessage(), CREATE_INSTITUTION_ERROR.getCode());
        }

    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }
}
