package it.pagopa.selfcare.auth.client;

import it.pagopa.selfcare.auth.filter.InternalUserMsHeaderFilter;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.internal_ms_user_json.api.UserApi;

@RegisterRestClient(configKey = "org.openapi.quarkus.internal_ms_user_json.api.UserApi")
@RegisterProvider(InternalUserMsHeaderFilter.class)
public interface InternalUserMsApi extends UserApi {
}
