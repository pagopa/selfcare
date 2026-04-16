package it.pagopa.selfcare.mscore.core.strategy;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.constant.CustomError;
import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.exception.ResourceConflictException;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
public class CreateInstitutionStrategyCommon {
    protected final InstitutionConnector institutionConnector;

    public CreateInstitutionStrategyCommon(InstitutionConnector institutionConnector) {
        this.institutionConnector = institutionConnector;
    }

    protected void checkIfAlreadyExistsByTaxCodeAndSubunitCode(String taxCode, String subunitCode) {
        List<Institution> institutions = institutionConnector.findByTaxCodeAndSubunitCode(taxCode, subunitCode, null);
        if (!institutions.isEmpty())
            throw new ResourceConflictException(String
                    .format(CustomError.CREATE_INSTITUTION_IPA_CONFLICT.getMessage(), taxCode, subunitCode),
                    CustomError.CREATE_INSTITUTION_CONFLICT.getCode());
    }

    protected static void setUpdatedFields(CreateInstitutionStrategyInput strategyInput, Institution toSavedOrUpdate) {
        if (strategyInput.getDescription() != null) {
            toSavedOrUpdate.setDescription(strategyInput.getDescription());
        }
        setIstatCode(strategyInput, toSavedOrUpdate);
        setContacts(strategyInput, toSavedOrUpdate);
        setLegalForm(strategyInput, toSavedOrUpdate);
        toSavedOrUpdate.setUpdatedAt(OffsetDateTime.now());
    }

    protected static void setIstatCode(CreateInstitutionStrategyInput strategyInput, Institution toSavedOrUpdate) {
        if (strategyInput.getIstatCode() != null) {
            toSavedOrUpdate.setIstatCode(strategyInput.getIstatCode());
        }
    }

    protected static void setLegalForm(CreateInstitutionStrategyInput strategyInput, Institution toSavedOrUpdate) {
        if (strategyInput.getLegalForm() != null) {
            toSavedOrUpdate.setLegalForm(strategyInput.getLegalForm());
        }
    }

    protected static void setContacts(CreateInstitutionStrategyInput strategyInput, Institution toSavedOrUpdate) {
        if (strategyInput.getSupportEmail() != null) {
            toSavedOrUpdate.setSupportEmail(strategyInput.getSupportEmail());
        }

        if (strategyInput.getSupportPhone() != null) {
            toSavedOrUpdate.setSupportPhone(strategyInput.getSupportPhone());
        }
    }

    protected static void setAdditionalDataForIpa(CreateInstitutionStrategyInput strategyInput, Institution toSavedOrUpdate) {
        if (strategyInput.getRea() != null) {
            toSavedOrUpdate.setRea(strategyInput.getRea());
        }

        if (strategyInput.getShareCapital() != null) {
            toSavedOrUpdate.setShareCapital(strategyInput.getShareCapital());
        }

        if (strategyInput.getBusinessRegisterPlace() != null) {
            toSavedOrUpdate.setBusinessRegisterPlace(strategyInput.getBusinessRegisterPlace());
        }
    }





}
