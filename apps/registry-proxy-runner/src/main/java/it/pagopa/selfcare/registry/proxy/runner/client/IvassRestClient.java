package it.pagopa.selfcare.registry.proxy.runner.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ivass")
@Path("/")
public interface IvassRestClient {

  @GET
  @Path("${ivass.data-path}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  byte[] retrieveInsurancesZip();
}
