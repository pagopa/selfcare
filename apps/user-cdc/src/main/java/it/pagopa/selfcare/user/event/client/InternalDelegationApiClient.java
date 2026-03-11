package it.pagopa.selfcare.user.event.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.internal_json.api.DelegationApi;

@RegisterRestClient(configKey = "client.internal.delegation-api")
@RegisterClientHeaders(InternalApiHeadersFactory.class)
public interface InternalDelegationApiClient extends DelegationApi {
}
