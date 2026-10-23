package it.pagopa.selfcare.product.model;

import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An (institutionType, origin) pair admitted to onboard the product.
 *
 * <p>A pair that is not listed is never admitted. A listed pair is admitted through the managed
 * flows (CONFIRMATION, IMPORT, aggregation, increment) and, unless {@link #selfOnboarding} is
 * {@code false}, through the self-service flow too.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OriginEntry {
  private InstitutionType institutionType;
  private Origin origin;
  private String labelKey;

  /**
   * Whether the pair can onboard through the self-service flow (frontend). {@code null} or {@code
   * true}: yes; {@code false}: only through the managed flows, and the pair is hidden from {@code
   * GET /product/origins}. A wrapper type on purpose: documents without the field stay
   * self-onboardable.
   */
  private Boolean selfOnboarding;
}
