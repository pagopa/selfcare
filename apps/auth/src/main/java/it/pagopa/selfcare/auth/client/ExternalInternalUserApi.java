package it.pagopa.selfcare.auth.client;

import it.pagopa.selfcare.auth.filter.ExternalInternalHeaderFilter;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.internal_json.api.UserApi;

@RegisterRestClient(configKey = "internal.user-api")
@RegisterProvider(ExternalInternalHeaderFilter.class)
public interface ExternalInternalUserApi extends UserApi {
}
