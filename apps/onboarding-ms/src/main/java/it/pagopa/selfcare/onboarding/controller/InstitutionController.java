package it.pagopa.selfcare.onboarding.controller;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.onboarding.controller.request.GetInstitutionRequest;
import it.pagopa.selfcare.onboarding.controller.response.InstitutionResponse;
import it.pagopa.selfcare.onboarding.service.InstitutionService;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Authenticated
@Path("/v1/institutions")
@Tag(name = "Institution Controller")
@AllArgsConstructor
@Slf4j
public class InstitutionController {

    private final InstitutionService institutionService;

    @Operation(
            summary = "Retrieve list of institutions given ids in input.",
            description = "Retrieve list of institutions given ids in input. If list in input is null" +
                    "or empty, a bad request exception is thrown"
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Multi<InstitutionResponse> getInstitutions(@Valid GetInstitutionRequest institutionRequest) {
        final List<String> institutionIds = institutionRequest.getInstitutionIds();
        return institutionService.getInstitutions(institutionIds);
    }


}
