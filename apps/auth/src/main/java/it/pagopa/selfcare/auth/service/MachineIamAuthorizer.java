package it.pagopa.selfcare.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.selfcare.auth.conf.TenantDefinition.MachineDefinition;
import it.pagopa.selfcare.auth.conf.TenantDefinition.MachineGrant;
import it.pagopa.selfcare.auth.exception.ForbiddenException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ServiceUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MachineIamAuthorizer {
  private static final Logger LOGGER = LoggerFactory.getLogger(MachineIamAuthorizer.class);
  private static final Duration TIMEOUT = Duration.ofSeconds(5);
  @Inject ObjectMapper mapper;
  HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(TIMEOUT)
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  public void requirePermission(
      String tenant, String uid, MachineDefinition machine, MachineGrant grant, String token) {
    URI uri =
        URI.create(
            machine.iamBaseUrl().replaceAll("/+$", "")
                + "/iam/users/"
                + uid
                + "/permissions/"
                + grant.scope()
                + "?productId="
                + grant.productId()
                + "&tenantId="
                + tenant);
    try {
      HttpResponse<String> response =
          http.send(
              HttpRequest.newBuilder(uri)
                  .timeout(TIMEOUT)
                  .header("Authorization", "Bearer " + token)
                  .header("X-Tenant-Id", tenant)
                  .header("Accept", "application/json")
                  .GET()
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 403 || response.statusCode() == 404) {
        throw new ForbiddenException("IAM user is not authorized");
      }
      if (response.statusCode() != 200 || response.body().length() > 16384) {
        throw unavailable(tenant);
      }
      JsonNode permission = mapper.readTree(response.body());
      if (permission == null || !permission.path("hasPermission").isBoolean()) {
        throw unavailable(tenant);
      }
      if (!permission.path("hasPermission").booleanValue()) {
        throw new ForbiddenException("IAM user is not authorized");
      }
    } catch (IOException exception) {
      throw unavailable(tenant);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw unavailable(tenant);
    }
  }

  private ServiceUnavailableException unavailable(String tenant) {
    LOGGER.warn("event=machine_iam_check_failed tenant={}", tenant);
    return new ServiceUnavailableException("IAM authorization is unavailable");
  }

  static URI requireHttps(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("M2M IAM base URL is required");
    }
    URI uri = URI.create(value);
    if (!"https".equals(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null) {
      throw new IllegalStateException("M2M IAM base URL must be HTTPS");
    }
    return uri;
  }
}
