package it.pagopa.selfcare.product.model.enums;

/** Where an onboarding request comes from. Declared server side by the calling service. */
public enum OnboardingChannel {
  /** Requested by the institution through the frontend. */
  SELF_SERVICE,
  /** Requested by PagoPA on behalf of the institution (confirmation, import, aggregation). */
  MANAGED
}
