package it.pagopa.selfcare.mscore.core;

import it.pagopa.selfcare.mscore.constant.RelationshipState;
import it.pagopa.selfcare.mscore.model.institution.GeographicTaxonomies;
import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ExternalServiceImpl implements ExternalService {

    private final InstitutionService institutionService;

    public ExternalServiceImpl(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @Override
    public Institution getInstitutionByExternalId(String externalId) {
        return institutionService.retrieveInstitutionByExternalId(externalId);
    }

    @Override
    public Institution retrieveInstitutionProduct(String externalId, String productId) {
        return institutionService.retrieveInstitutionProduct(externalId, productId);
    }

    @Override
    public List<Onboarding> retrieveInstitutionProductsByExternalId(String externalId, List<RelationshipState> states) {
        Institution institution = institutionService.retrieveInstitutionByExternalId(externalId);
        return institutionService.retrieveInstitutionProducts(institution, states);
    }

    @Override
    public List<Institution> retrieveInstitutionByIds(List<String> ids) {
        return institutionService.retrieveInstitutionByIds(ids);
    }

    @Override
    public List<GeographicTaxonomies> retrieveInstitutionGeoTaxonomiesByExternalId(String externalId) {
        log.info("Retrieving geographic taxonomies for institution having externalId {}", externalId);
        Institution institution = institutionService.retrieveInstitutionByExternalId(externalId);
        return institutionService.retrieveInstitutionGeoTaxonomies(institution);
    }
}
