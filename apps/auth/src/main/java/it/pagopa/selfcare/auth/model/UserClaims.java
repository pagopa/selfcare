package it.pagopa.selfcare.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserClaims {
  private String uid;
  private String fiscalCode;
  private String name;
  private String familyName;
  private String email;
  private Boolean sameIdp = Boolean.TRUE;
  private Boolean test = Boolean.FALSE;
  /** Tenant resolved from the {@code X-Tenant-Id} header (Step_0 SELC-4); embedded as the JWT
   * {@code tenant_id} claim by {@code SessionService}. */
  private String tenantId;
}
