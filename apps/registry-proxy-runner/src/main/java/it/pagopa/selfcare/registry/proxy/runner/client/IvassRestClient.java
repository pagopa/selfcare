package it.pagopa.selfcare.registry.proxy.runner.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ivass")
public interface IvassRestClient extends DataSourceRestClient<byte[]> {

  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  byte[] retrieveDataSource();
}
