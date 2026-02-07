package it.pagopa.selfcare.auth.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.internal_ms_user_json.api.UserApi;

@RegisterRestClient(configKey = "internal.user-ms.api")
@RegisterClientHeaders(InternalUserMsHeaderFactory.class)
public interface InternalUserMsApi extends UserApi {}
