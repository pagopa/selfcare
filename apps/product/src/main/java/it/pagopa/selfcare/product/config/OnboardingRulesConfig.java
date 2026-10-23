package it.pagopa.selfcare.product.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import java.util.Map;
import java.util.Set;

/** Platform-wide onboarding rules, shared by every product. */
@ConfigMapping(prefix = "product-ms.onboarding-rules")
public interface OnboardingRulesConfig {

  /** What happens when a product is saved with incoherent onboarding rules. */
  @WithDefault("WARN")
  ValidationMode validationMode();

  /** Institution types each origin can provide. Origins not listed are not checked. */
  Map<Origin, Set<InstitutionType>> allowedInstitutionTypes();

  /**
   * Workflows admitted for each origin. Origins not listed are not checked; products with a
   * parent are never checked.
   */
  Map<Origin, Set<WorkflowType>> allowedWorkflowTypes();

  enum ValidationMode {
    /** Log the problems and save the product. */
    WARN,
    /** Reject the product with 400. */
    STRICT
  }
}
