package it.pagopa.selfcare.onboarding.service;

import io.jsonwebtoken.Claims;

/**
 * Identity of the caller of an HTTP-triggered function, built only from the claims of the verified
 * JWT. {@code clientId}, {@code subscriptionId} and {@code productId} identify the entry channel
 * (e.g. frontend session or APIM subscription) and are {@code null} when the issuer does not set
 * them.
 */
public record CallerIdentity(
    String userId, String issuer, String clientId, String subscriptionId, String productId) {

  static final String UID_CLAIM = "uid";
  static final String CLIENT_ID_CLAIM = "client_id";
  static final String SUBSCRIPTION_ID_CLAIM = "subscription_id";
  static final String PRODUCT_ID_CLAIM = "product_id";

  static CallerIdentity fromClaims(Claims claims) {
    return new CallerIdentity(
        claims.get(UID_CLAIM, String.class),
        claims.getIssuer(),
        stringClaim(claims, CLIENT_ID_CLAIM),
        stringClaim(claims, SUBSCRIPTION_ID_CLAIM),
        stringClaim(claims, PRODUCT_ID_CLAIM));
  }

  private static String stringClaim(Claims claims, String name) {
    Object value = claims.get(name);
    if (value == null) {
      return null;
    }
    String text = value.toString().trim();
    return text.isEmpty() ? null : text;
  }
}
