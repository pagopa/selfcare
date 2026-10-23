package it.pagopa.selfcare.product.service;

import it.pagopa.selfcare.product.config.OnboardingRulesConfig;
import it.pagopa.selfcare.product.config.OnboardingRulesConfig.ValidationMode;
import it.pagopa.selfcare.product.model.OriginEntry;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.WorkflowRule;
import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.owasp.encoder.Encode;

/**
 * Single source of truth for the onboarding admission rules: which (institutionType, origin) pairs
 * can join a product, through which channel and with which workflow.
 *
 * <p>Rules combine the platform-wide {@link OnboardingRulesConfig} (origin coherence, workflows
 * admitted per origin) with the product configuration ({@code institutionOrigins}, {@code
 * workflowRules}, {@code signingConfiguration}, {@code parentId}). The same rules are used to
 * decide a single onboarding and to validate a product configuration when it is saved, so a
 * product that passes validation cannot produce an inconsistent decision.
 */
@Slf4j
@ApplicationScoped
public class OnboardingRules {

  static final String ORIGIN_MISMATCH =
      "InstitutionType '%s' is not allowed for origin '%s'. Allowed institution types: [%s]";
  static final String INVALID_RULES = "Invalid onboarding rules for product %s: %s";

  private final OnboardingRulesConfig config;

  @Inject
  public OnboardingRules(OnboardingRulesConfig config) {
    this.config = config;
  }

  /**
   * A listed pair is self-onboardable unless {@code selfOnboarding} is explicitly {@code false}.
   */
  public static boolean isSelfOnboardingAllowed(OriginEntry entry) {
    return !Boolean.FALSE.equals(entry.getSelfOnboarding());
  }

  /**
   * Checks that the origin can provide the institution type (e.g. IPA cannot provide SA).
   *
   * @return the error message when the pair is incoherent, empty otherwise or when the origin has
   *     no configured constraint
   */
  public Optional<String> checkOriginCoherence(InstitutionType institutionType, Origin origin) {
    Set<InstitutionType> allowed = config.allowedInstitutionTypes().get(origin);
    if (Objects.isNull(allowed) || allowed.contains(institutionType)) {
      return Optional.empty();
    }
    String allowedTypes = allowed.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
    return Optional.of(
        String.format(ORIGIN_MISMATCH, institutionType.name(), origin.name(), allowedTypes));
  }

  /** Returns the {@code institutionOrigins} entry matching the pair, if any. */
  public Optional<OriginEntry> findOriginEntry(
      Product product, InstitutionType institutionType, Origin origin) {
    return Optional.ofNullable(product.getInstitutionOrigins()).orElse(List.of()).stream()
        .filter(entry -> institutionType == entry.getInstitutionType())
        .filter(entry -> origin == entry.getOrigin())
        .findFirst();
  }

  /**
   * Resolves the workflow of an onboarding whose workflow is not imposed by the operation. The
   * first matching rule wins:
   *
   * <ol>
   *   <li>institution type PT: {@code FOR_APPROVE_PT};
   *   <li>aggregator: {@code CONTRACT_REGISTRATION_AGGREGATOR};
   *   <li>more than one required signature: {@code CONTRACT_WITH_COUNTERSIGNATURE};
   *   <li>product with a parent: {@code CONTRACT_REGISTRATION};
   *   <li>the {@code workflowRules} entry matching the pair.
   * </ol>
   *
   * @return the workflow, empty when none of the rules applies
   */
  public Optional<WorkflowType> resolveWorkflowType(
      Product product, InstitutionType institutionType, Origin origin, boolean aggregator) {
    if (InstitutionType.PT == institutionType) {
      return Optional.of(WorkflowType.FOR_APPROVE_PT);
    }
    if (aggregator) {
      return Optional.of(WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR);
    }
    if (Objects.nonNull(product.getSigningConfiguration())
        && product.getSigningConfiguration().getRequiredSignatures() > 1) {
      return Optional.of(WorkflowType.CONTRACT_WITH_COUNTERSIGNATURE);
    }
    if (hasParent(product)) {
      return Optional.of(WorkflowType.CONTRACT_REGISTRATION);
    }
    return Optional.ofNullable(product.getWorkflowRules()).orElse(List.of()).stream()
        .filter(rule -> institutionType == rule.getInstitutionType())
        .filter(rule -> origin == rule.getOrigin())
        .map(WorkflowRule::getWorkflowType)
        .filter(Objects::nonNull)
        .findFirst();
  }

  /**
   * Checks the workflow against the workflows admitted for the origin. Products with a parent are
   * not checked, as their workflow follows the parent onboarding.
   */
  public boolean isWorkflowAllowedForOrigin(
      Product product, Origin origin, WorkflowType workflowType) {
    if (hasParent(product)) {
      return true;
    }
    Set<WorkflowType> allowed = config.allowedWorkflowTypes().get(origin);
    return Objects.isNull(allowed) || allowed.contains(workflowType);
  }

  /**
   * Validates the onboarding configuration of a product.
   *
   * @return the problems found, empty when the configuration is coherent
   */
  public List<String> validate(Product product) {
    List<String> problems = new ArrayList<>();
    Set<String> pairs = new HashSet<>();
    for (OriginEntry entry : Optional.ofNullable(product.getInstitutionOrigins()).orElse(List.of())) {
      validateOriginEntry(product, entry, pairs, problems);
    }
    Set<String> rulePairs = new HashSet<>();
    for (WorkflowRule rule : Optional.ofNullable(product.getWorkflowRules()).orElse(List.of())) {
      validateWorkflowRule(rule, pairs, rulePairs, problems);
    }
    return problems;
  }

  /**
   * Validates the product and, depending on {@link OnboardingRulesConfig#validationMode()}, logs
   * the problems (WARN) or rejects the product with {@link BadRequestException} (STRICT).
   */
  public void enforce(Product product) {
    List<String> problems = validate(product);
    if (problems.isEmpty()) {
      return;
    }
    String message =
        String.format(
            INVALID_RULES, Encode.forJava(product.getProductId()), String.join("; ", problems));
    if (ValidationMode.STRICT == config.validationMode()) {
      throw new BadRequestException(message);
    }
    log.warn(message);
  }

  private void validateOriginEntry(
      Product product, OriginEntry entry, Set<String> pairs, List<String> problems) {
    InstitutionType institutionType = entry.getInstitutionType();
    Origin origin = entry.getOrigin();
    if (Objects.isNull(institutionType) || Objects.isNull(origin)) {
      problems.add("institutionOrigins entry without institutionType or origin");
      return;
    }
    String pair = institutionType + "/" + origin;
    if (!pairs.add(pair)) {
      problems.add("institutionOrigins " + pair + " is duplicated");
    }
    checkOriginCoherence(institutionType, origin)
        .ifPresent(message -> problems.add("institutionOrigins " + pair + ": " + message));
    if (!isSelfOnboardingAllowed(entry)) {
      return;
    }
    Optional<WorkflowType> workflowType =
        resolveWorkflowType(product, institutionType, origin, false);
    if (workflowType.isEmpty()) {
      problems.add("institutionOrigins " + pair + " is self-onboardable but has no workflow");
    } else if (!isWorkflowAllowedForOrigin(product, origin, workflowType.get())) {
      problems.add(
          "institutionOrigins "
              + pair
              + " resolves workflow "
              + workflowType.get()
              + " not allowed for origin "
              + origin);
    }
  }

  private void validateWorkflowRule(
      WorkflowRule rule, Set<String> pairs, Set<String> rulePairs, List<String> problems) {
    if (Objects.isNull(rule.getInstitutionType())
        || Objects.isNull(rule.getOrigin())
        || Objects.isNull(rule.getWorkflowType())) {
      problems.add("workflowRules entry without institutionType, origin or workflowType");
      return;
    }
    String pair = rule.getInstitutionType() + "/" + rule.getOrigin();
    if (!rulePairs.add(pair)) {
      problems.add("workflowRules " + pair + " is duplicated");
    }
    if (!pairs.contains(pair)) {
      problems.add("workflowRules " + pair + " has no institutionOrigins entry");
    }
  }

  private static boolean hasParent(Product product) {
    return StringUtils.isNotBlank(product.getParentId());
  }
}
