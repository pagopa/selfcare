package it.pagopa.selfcare.registry.proxy.runner.client;

import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexRequest;
import it.pagopa.selfcare.registry.proxy.runner.model.SearchServiceIndexResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "azure-ai-search")
@Path("/")
public interface AzureSearchRestClient {

  @POST
  @Path("indexes/{indexName}/docs/index")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  Object indexIpaInstitutions(
      @PathParam("indexName") String indexName,
      @QueryParam("api-version") String apiVersion,
      SearchServiceIndexRequest request);

  @GET
  @Path("indexes/{indexName}/docs")
  @Produces(MediaType.APPLICATION_JSON)
  SearchServiceIndexResponse searchIpaInstitutions(
      @PathParam("indexName") String indexName,
      @QueryParam("api-version") String apiVersion,
      @QueryParam("search") String search,
      @QueryParam("$select") String select,
      @QueryParam("$count") Boolean count,
      @QueryParam("$top") Integer top,
      @QueryParam("$skip") Integer skip,
      @QueryParam("$filter") String filter);
}
