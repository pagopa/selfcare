package it.pagopa.selfcare.mscore.core.strategy.factory;

import it.pagopa.selfcare.mscore.api.InstitutionConnector;
import it.pagopa.selfcare.mscore.api.PartyRegistryProxyConnector;
import it.pagopa.selfcare.mscore.core.mapper.InstitutionMapper;
import it.pagopa.selfcare.mscore.core.strategy.*;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CreateInstitutionStrategyFactory {

    private final InstitutionConnector institutionConnector;
    private final PartyRegistryProxyConnector partyRegistryProxyConnector;
    private final InstitutionMapper institutionMapper;

    public CreateInstitutionStrategyFactory(InstitutionConnector institutionConnector,
                                            PartyRegistryProxyConnector partyRegistryProxyConnector,
                                            InstitutionMapper institutionMapper) {
        this.institutionConnector = institutionConnector;
        this.partyRegistryProxyConnector = partyRegistryProxyConnector;
        this.institutionMapper = institutionMapper;
    }

    public CreateInstitutionStrategy createInstitutionStrategy(Institution institution) {
        CreateInstitutionStrategyRaw strategy = new CreateInstitutionStrategyRaw(institutionConnector);
        strategy.setInstitution(institution);
        return strategy;
    }

    public CreateInstitutionStrategy createInstitutionStrategyIpa() {
        return new CreateInstitutionStrategyIpa(partyRegistryProxyConnector, institutionConnector, institutionMapper);
    }

    public CreateInstitutionStrategy createInstitutionStrategyAnac(Institution institution) {
        CreateInstitutionStrategyAnac strategy = new CreateInstitutionStrategyAnac(partyRegistryProxyConnector, institutionConnector);
        strategy.setInstitution(institution);
        return strategy;
    }
    public CreateInstitutionStrategy createInstitutionStrategyIvass(Institution institution) {
        CreateInstitutionStrategyIvass strategy = new CreateInstitutionStrategyIvass(partyRegistryProxyConnector, institutionConnector);
        strategy.setInstitution(institution);
        return strategy;
    }

    public CreateInstitutionStrategy createInstitutionStrategyPda(String injectionInstitutionType) {
        CreateInstitutionStrategyPda strategy = new CreateInstitutionStrategyPda(partyRegistryProxyConnector, institutionConnector, institutionMapper);
        strategy.setInjectionInstitutionType(injectionInstitutionType);
        return strategy;
    }

    public CreateInstitutionStrategy createInstitutionStrategyInfocamere(Institution institution) {
        CreateInstitutionStrategyInfocamere strategy =  new CreateInstitutionStrategyInfocamere(partyRegistryProxyConnector, institutionConnector);
        strategy.setInstitution(institution);
        return strategy;
    }

}
