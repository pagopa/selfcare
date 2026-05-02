package it.pagopa.selfcare.registry.proxy.runner.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "anac")
@Path("/")
public interface AnacRestClient {

  @GET
  @Path("${anac.data-path}")
  @Produces(MediaType.TEXT_PLAIN)
  String retrieveStations();
}
