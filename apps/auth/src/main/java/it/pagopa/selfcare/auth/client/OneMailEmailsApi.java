package it.pagopa.selfcare.auth.client;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.one_mail_json.api.EmailsApi;

@RegisterRestClient(configKey = "one_mail.api")
@RegisterClientHeaders(OneMailEmailsHeaderFactory.class)
public interface OneMailEmailsApi extends EmailsApi {}
