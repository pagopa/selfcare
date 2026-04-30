package it.pagopa.selfcare.mscore.core.strategy;

import it.pagopa.selfcare.mscore.core.strategy.input.CreateInstitutionStrategyInput;
import it.pagopa.selfcare.mscore.model.institution.Institution;

import java.util.Objects;

public interface CreateInstitutionStrategy {

    String TYPE_MAIL_PEC = "Pec";

    default String getExternalId(CreateInstitutionStrategyInput strategyInput) {
        return Objects.isNull(strategyInput.getSubunitCode())
                ? strategyInput.getTaxCode()
                : String.format("%s_%s", strategyInput.getTaxCode(), strategyInput.getSubunitCode());
    }

    Institution createInstitution(CreateInstitutionStrategyInput strategyInput);

}
