package it.pagopa.selfcare.auth.filter;

import jakarta.ws.rs.client.ClientRequestContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApimHeaderFilter {

  private static final String HEADER_NAME = "Ocp-Apim-Subscription-Key";
  public void injectApimKey(ClientRequestContext requestContext, String apiKey) {
      requestContext.getHeaders().putSingle(HEADER_NAME, apiKey);
  }
}
