package it.pagopa.selfcare.user.event.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.internal_json.api.UserApi;

@RegisterRestClient(configKey = "client.internal.user-api")
@RegisterClientHeaders(InternalApiHeadersFactory.class)
public interface InternalUserApiClient extends UserApi {
}
