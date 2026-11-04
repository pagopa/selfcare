package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.HttpResponseMessage;
import java.util.Objects;

/**
 * Outcome of the authentication of an HTTP-triggered function request: either the response to send
 * back immediately ({@link #rejection()}) or the verified {@link #caller()}.
 */
public record HttpAuthorization(HttpResponseMessage rejection, CallerIdentity caller) {

  public static HttpAuthorization rejected(HttpResponseMessage rejection) {
    return new HttpAuthorization(Objects.requireNonNull(rejection), null);
  }

  public static HttpAuthorization authorized(CallerIdentity caller) {
    return new HttpAuthorization(null, Objects.requireNonNull(caller));
  }

  public boolean isRejected() {
    return rejection != null;
  }
}
