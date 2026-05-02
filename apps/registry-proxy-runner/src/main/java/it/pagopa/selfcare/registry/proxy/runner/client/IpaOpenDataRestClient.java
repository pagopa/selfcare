package it.pagopa.selfcare.registry.proxy.runner.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "ipa-open-data")
@Path("/")
public interface IpaOpenDataRestClient {

  @GET
  @Path("${ipa-open-data.institutions-path}")
  @Produces(MediaType.TEXT_PLAIN)
  String retrieveInstitutions();

  @GET
  @Path("${ipa-open-data.categories-path}")
  @Produces(MediaType.TEXT_PLAIN)
  String retrieveCategories();

  @GET
  @Path("${ipa-open-data.aoos-path}")
  @Produces(MediaType.TEXT_PLAIN)
  String retrieveAOOs();

  @GET
  @Path("${ipa-open-data.uos-path}")
  @Produces(MediaType.TEXT_PLAIN)
  String retrieveUOs();

  @GET
  @Path("${ipa-open-data.uos-sfe-path}")
  @Produces(MediaType.TEXT_PLAIN)
  String retrieveUOsWithSfe();
}
