package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.model.*;
import org.jboss.resteasy.reactive.RestResponse;

import java.io.File;

public interface AggregatesService {

    Uni<VerifyAggregateResponse> validateAppIoAggregatesCsv (File file);

    Uni<VerifyAggregateResponse> validatePagoPaAggregatesCsv (File file);

    Uni<VerifyAggregateResponse> validateSendAggregatesCsv (File file);

    Uni<RestResponse<File>> retrieveAggregatesCsv(String onboardingId, String productId);

}
