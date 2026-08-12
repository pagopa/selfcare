package it.pagopa.selfcare.auth.context;

import jakarta.enterprise.context.RequestScoped;
import lombok.Getter;

/**
 * Carries, for the lifetime of a session-issuing request, the token {@code auth} has just minted
 * and the tenant embedded in its {@code tenant_id} claim (Step_0 SELC-4).
 *
 * <p>Both are needed by {@link it.pagopa.selfcare.auth.client.IamMsHeadersFactory}: downstream
 * services validate the {@code X-Tenant-Id} header against the token claim and reject any
 * mismatch, so the header must be derived from the very token being sent — never from the
 * incoming request independently.
 */
@RequestScoped
@Getter
public class TokenContext {
  private String token;
  private String tenantId;

  public String setToken(String token) {
    this.token = token;
    return token;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }
}
