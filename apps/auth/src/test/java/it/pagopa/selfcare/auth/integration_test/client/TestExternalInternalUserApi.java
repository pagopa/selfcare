package it.pagopa.selfcare.auth.integration_test.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.internal_json.api.UserApi;

@RegisterRestClient(configKey = "internal.external-ms")
@RegisterClientHeaders(TestExternalInternalUserHeaderFactory.class)
public interface TestExternalInternalUserApi extends UserApi {
}