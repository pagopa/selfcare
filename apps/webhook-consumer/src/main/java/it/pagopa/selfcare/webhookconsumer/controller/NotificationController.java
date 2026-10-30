package it.pagopa.selfcare.webhookconsumer.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

/**
 * Test endpoint registered as webhook URL: logs every received notification (payload and
 * Authorization header) and acknowledges it.
 */
@Slf4j
@Path("/notifications")
@Consumes(MediaType.WILDCARD)
public class NotificationController {

  @POST
  public Response receivePost(
      @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization, String payload) {
    return receive("POST", authorization, payload);
  }

  @PUT
  public Response receivePut(
      @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization, String payload) {
    return receive("PUT", authorization, payload);
  }

  @PATCH
  public Response receivePatch(
      @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization, String payload) {
    return receive("PATCH", authorization, payload);
  }

  private Response receive(String method, String authorization, String payload) {
    log.info(
        "Webhook notification received - method: {}, Authorization: {}, payload: {}",
        method,
        sanitize(authorization),
        sanitize(payload));
    return Response.noContent().build();
  }

  // Escape line breaks to prevent log forging through untrusted input.
  private static String sanitize(String value) {
    return value == null ? null : value.replace("\r", "\\r").replace("\n", "\\n");
  }
}
