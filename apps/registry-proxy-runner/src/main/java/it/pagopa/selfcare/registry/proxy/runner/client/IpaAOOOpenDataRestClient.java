package it.pagopa.selfcare.registry.proxy.runner.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ipa-open-data-aoo")
public interface IpaAOOOpenDataRestClient extends DataSourceRestClient<String> {

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  String retrieveDataSource();
}
