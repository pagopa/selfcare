package it.pagopa.selfcare.product.model.enums;

/** Why an onboarding decision denied the request, in the order the checks are evaluated. */
public enum OnboardingDecisionReason {
  /** The institutionType cannot come from the origin (e.g. SA from IPA). */
  ORIGIN_INSTITUTION_TYPE_MISMATCH,
  /** The (institutionType, origin) pair is not listed in the product institutionOrigins. */
  INSTITUTION_TYPE_NOT_ALLOWED,
  /** The pair is admitted only through managed flows and the request is self-service. */
  SELF_ONBOARDING_NOT_ALLOWED,
  /** No workflow can be resolved for the pair. */
  WORKFLOW_NOT_RESOLVABLE,
  /** The resolved workflow is not admitted for the origin. */
  WORKFLOW_NOT_ALLOWED_FOR_ORIGIN
}
