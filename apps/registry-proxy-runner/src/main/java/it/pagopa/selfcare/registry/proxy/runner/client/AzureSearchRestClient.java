package it.pagopa.selfcare.registry.proxy.runner.client;

import it.pagopa.selfcare.registry.proxy.runner.model.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "azure-ai-search")
@RegisterClientHeaders(AzureSearchHeadersFactory.class)
@Path("/")
public interface AzureSearchRestClient {

  @POST
  @Path("indexes/{indexName}/docs/index")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  Object index(
      @PathParam("indexName") String indexName,
      @QueryParam("api-version") String apiVersion,
      SearchServiceIndexRequest request);

  @GET
  @Path("indexes/{indexName}/docs")
  @Produces(MediaType.APPLICATION_JSON)
  SearchServiceIndexResponse search(
      @PathParam("indexName") String indexName,
      @QueryParam("api-version") String apiVersion,
      @QueryParam("search") String search,
      @QueryParam("$select") String select,
      @QueryParam("$count") Boolean count,
      @QueryParam("$top") Integer top,
      @QueryParam("$skip") Integer skip,
      @QueryParam("$filter") String filter);

  @GET
  @Path("indexes/{indexName}/docs")
  @Produces(MediaType.APPLICATION_JSON)
  OnboardingSearchResponse searchOnboarding(
          @PathParam("indexName") String indexName,
          @QueryParam("api-version") String apiVersion,
          @QueryParam("search") String search,
          @QueryParam("$select") String select,
          @QueryParam("$count") Boolean count,
          @QueryParam("$top") Integer top,
          @QueryParam("$skip") Integer skip,
          @QueryParam("$filter") String filter);
}