package it.pagopa.selfcare.user.event.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.internal_json.api.UserGroupApi;

@RegisterRestClient(configKey = "client.internal.user-group-api")
@RegisterClientHeaders(InternalApiHeadersFactory.class)
public interface InternalUserGroupApiClient extends UserGroupApi {
}
