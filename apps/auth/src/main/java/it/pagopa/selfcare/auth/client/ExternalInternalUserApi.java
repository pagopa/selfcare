package it.pagopa.selfcare.auth.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.internal_json.api.UserApi;

@RegisterRestClient(configKey = "internal.user-api")
@RegisterClientHeaders(ExternalInternalUserHeaderFactory.class)
public interface ExternalInternalUserApi extends UserApi {}
