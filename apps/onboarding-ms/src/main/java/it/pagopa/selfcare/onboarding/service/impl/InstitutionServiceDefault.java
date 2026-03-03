package it.pagopa.selfcare.onboarding.service.impl;

import static it.pagopa.selfcare.onboarding.util.QueryUtils.FieldNames.INSTITUTION_ID;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import it.pagopa.selfcare.onboarding.util.QueryUtils;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.openapi.quarkus.core_json.api.InstitutionApi;
import org.openapi.quarkus.core_json.model.InstitutionsResponse;

@Slf4j
@ApplicationScoped
public class InstitutionServiceDefault implements InstitutionService {

    private final InstitutionApi institutionApi;

    public InstitutionServiceDefault(@RestClient InstitutionApi institutionApi) {
        this.institutionApi = institutionApi;
    }

    @Override
    public Uni<InstitutionsResponse> getInstitutionsUsingGET(String taxCode,
                                                             String subunitCode,
                                                             String origin,
                                                             String originId,
                                                             String productId,
                                                             Boolean enableSubunits) {
        log.info("Calling institutionApi.getInstitutionsUsingGET");
        log.debug("getInstitutionsUsingGET params: taxCode={}, subunitCode={}, origin={}, originId={}, productId={}, enableSubunits={}",
                taxCode, subunitCode, origin, originId, productId, enableSubunits);

        return institutionApi.getInstitutionsUsingGET(taxCode,
                subunitCode,
                origin,
                originId,
                productId,
                enableSubunits);
    }

    @Override
    public Multi<InstitutionResponse> getInstitutions(List<String> institutionIds) {
        if (Objects.isNull(institutionIds) || institutionIds.isEmpty()) {
            log.error("The parameter institutionIds cannot be null or empty");
            return Multi.createFrom().empty();
        }
        Map<String, Object> queryParameterMap = new HashMap<>();
        queryParameterMap.put(INSTITUTION_ID, institutionIds);
        Document query = QueryUtils.buildQuery(queryParameterMap);
        return getDistinctOnboardings(query).onItem().transform(onboarding -> {
            InstitutionResponse institutionResponse = new InstitutionResponse();
            institutionResponse.setId(onboarding.getInstitution().getId());
            institutionResponse.setInstitutionType(onboarding.getInstitution().getInstitutionType().name());
            institutionResponse.setDescription(onboarding.getInstitution().getDescription());
            return institutionResponse;
        });
    }

    public Multi<Onboarding> getDistinctOnboardings(Document query) {
        Set<String> seenInstitutionIds = new HashSet<>();
        return Onboarding.find(query).stream().map(Onboarding.class::cast)
                .filter(onboarding -> seenInstitutionIds.add(onboarding.getInstitution().getId()));
    }
}
