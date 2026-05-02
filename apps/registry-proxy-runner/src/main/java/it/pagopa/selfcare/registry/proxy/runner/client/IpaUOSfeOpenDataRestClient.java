package it.pagopa.selfcare.registry.proxy.runner.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ipa-open-data")
@Path("/")
public interface IpaUOSfeOpenDataRestClient extends DataSourceRestClient<String> {

  @GET
  @Path("${ipa-open-data.uos-sfe-path}")
  @Produces(MediaType.TEXT_PLAIN)
  String retrieveDataSource();
}
