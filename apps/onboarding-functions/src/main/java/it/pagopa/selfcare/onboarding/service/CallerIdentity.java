package it.pagopa.selfcare.onboarding.service;

import io.jsonwebtoken.Claims;

/**
 * Identity of the caller of an HTTP-triggered function, built only from the claims of the verified
 * JWT. {@code clientId}, {@code subscriptionId} and {@code productId} identify the entry channel
 * (e.g. frontend session or APIM subscription) and are {@code null} when the issuer does not set
 * them. {@code tenant} is the verified tenant of the caller (the Personal Data Vault that owns
 * {@code userId}); {@code tenantSource} tells whether it comes from the {@code tenant_id} claim or
 * from the configured default for tokens without it.
 */
public record CallerIdentity(
    String userId,
    String issuer,
    String clientId,
    String subscriptionId,
    String productId,
    String tenant,
    String tenantSource) {

  static final String UID_CLAIM = "uid";
  static final String CLIENT_ID_CLAIM = "client_id";
  static final String SUBSCRIPTION_ID_CLAIM = "subscription_id";
  static final String PRODUCT_ID_CLAIM = "product_id";
  public static final String TENANT_SOURCE_CLAIM = "claim";
  public static final String TENANT_SOURCE_DEFAULT = "default";

  static CallerIdentity fromClaims(Claims claims, String tenant, String tenantSource) {
    return new CallerIdentity(
        claims.get(UID_CLAIM, String.class),
        claims.getIssuer(),
        stringClaim(claims, CLIENT_ID_CLAIM),
        stringClaim(claims, SUBSCRIPTION_ID_CLAIM),
        stringClaim(claims, PRODUCT_ID_CLAIM),
        tenant,
        tenantSource);
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
