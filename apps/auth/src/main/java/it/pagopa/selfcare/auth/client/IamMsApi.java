package it.pagopa.selfcare.auth.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.iam_json.api.IamApi;

@RegisterRestClient(configKey = "iam.api")
@RegisterClientHeaders(IamMsHeadersFactory.class)
public interface IamMsApi extends IamApi {}
