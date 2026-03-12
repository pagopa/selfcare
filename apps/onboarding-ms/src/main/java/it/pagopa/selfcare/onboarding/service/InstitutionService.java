package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import org.openapi.quarkus.core_json.model.InstitutionsResponse;

import java.util.List;

public interface InstitutionService {

    Uni<InstitutionsResponse> getInstitutionsUsingGET(String taxCode,
                                                      String subunitCode,
                                                      String origin,
                                                      String originId,
                                                      String productId,
                                                      Boolean enableSubunits);

    Multi<InstitutionResponse> getInstitutions(List<String> institutionIds);
}
