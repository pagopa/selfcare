package it.pagopa.selfcare.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.quarkus.test.junit.QuarkusTest;
import it.pagopa.selfcare.product.config.OnboardingRulesConfig;
import it.pagopa.selfcare.product.config.OnboardingRulesConfig.ValidationMode;
import it.pagopa.selfcare.product.exception.InvalidRequestException;
import it.pagopa.selfcare.product.model.OriginEntry;
import it.pagopa.selfcare.product.model.Product;
import it.pagopa.selfcare.product.model.SigningConfiguration;
import it.pagopa.selfcare.product.model.WorkflowRule;
import it.pagopa.selfcare.product.model.enums.InstitutionType;
import it.pagopa.selfcare.product.model.enums.Origin;
import it.pagopa.selfcare.product.model.enums.WorkflowType;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OnboardingRulesTest {

  @Inject OnboardingRules onboardingRules;

  private static OriginEntry entry(InstitutionType type, Origin origin, Boolean selfOnboarding) {
    return OriginEntry.builder()
        .institutionType(type)
        .origin(origin)
        .selfOnboarding(selfOnboarding)
        .build();
  }

  private static WorkflowRule rule(InstitutionType type, Origin origin, WorkflowType workflow) {
    return WorkflowRule.builder().institutionType(type).origin(origin).workflowType(workflow).build();
  }

  private static OnboardingRules strictRules() {
    return rules(ValidationMode.STRICT);
  }

  private static OnboardingRules rules(ValidationMode mode) {
    OnboardingRulesConfig config = mock(OnboardingRulesConfig.class);
    when(config.validationMode()).thenReturn(mode);
    when(config.allowedInstitutionTypes()).thenReturn(Map.of());
    when(config.allowedWorkflowTypes()).thenReturn(Map.of());
    return new OnboardingRules(config);
  }

  @Test
  void isSelfOnboardingAllowed_isFalseOnlyWhenExplicitlyFalse() {
    assertThat(OnboardingRules.isSelfOnboardingAllowed(entry(null, null, null))).isTrue();
    assertThat(OnboardingRules.isSelfOnboardingAllowed(entry(null, null, true))).isTrue();
    assertThat(OnboardingRules.isSelfOnboardingAllowed(entry(null, null, false))).isFalse();
  }

  @Test
  void checkOriginCoherence_acceptsTypeProvidedByOrigin() {
    assertThat(onboardingRules.checkOriginCoherence(InstitutionType.GPU, Origin.SELC)).isEmpty();
  }

  @Test
  void checkOriginCoherence_rejectsTypeNotProvidedByOrigin() {
    assertThat(onboardingRules.checkOriginCoherence(InstitutionType.SA, Origin.IPA))
        .contains(
            "InstitutionType 'SA' is not allowed for origin 'IPA'. Allowed institution types:"
                + " [GSP, PA, SCEC]");
  }

  @Test
  void checkOriginCoherence_acceptsAnyTypeWhenOriginIsNotConfigured() {
    assertThat(strictRules().checkOriginCoherence(InstitutionType.SA, Origin.IPA)).isEmpty();
  }

  @Test
  void findOriginEntry_matchesBothTypeAndOrigin() {
    OriginEntry gpuSelc = entry(InstitutionType.GPU, Origin.SELC, false);
    Product product =
        Product.builder()
            .institutionOrigins(
                List.of(entry(InstitutionType.GPU, Origin.IPA, null), gpuSelc))
            .build();

    assertThat(onboardingRules.findOriginEntry(product, InstitutionType.GPU, Origin.SELC))
        .contains(gpuSelc);
    assertThat(onboardingRules.findOriginEntry(product, InstitutionType.PA, Origin.SELC)).isEmpty();
  }

  @Test
  void findOriginEntry_isEmptyWhenProductHasNoOrigins() {
    assertThat(
            onboardingRules.findOriginEntry(
                Product.builder().build(), InstitutionType.PA, Origin.IPA))
        .isEmpty();
  }

  @Test
  void resolveWorkflowType_appliesPrioritiesInOrder() {
    Product countersigned =
        Product.builder()
            .parentId("prod-parent")
            .signingConfiguration(SigningConfiguration.builder().requiredSignatures(2).build())
            .workflowRules(List.of(rule(InstitutionType.PA, Origin.IPA, WorkflowType.FOR_APPROVE)))
            .build();

    assertThat(
            onboardingRules.resolveWorkflowType(
                countersigned, InstitutionType.PT, Origin.SELC, true))
        .contains(WorkflowType.FOR_APPROVE_PT);
    assertThat(
            onboardingRules.resolveWorkflowType(countersigned, InstitutionType.PA, Origin.IPA, true))
        .contains(WorkflowType.CONTRACT_REGISTRATION_AGGREGATOR);
    assertThat(
            onboardingRules.resolveWorkflowType(
                countersigned, InstitutionType.PA, Origin.IPA, false))
        .contains(WorkflowType.CONTRACT_WITH_COUNTERSIGNATURE);
  }

  @Test
  void resolveWorkflowType_usesParentWhenSingleSignature() {
    Product child =
        Product.builder()
            .parentId("prod-parent")
            .signingConfiguration(SigningConfiguration.builder().requiredSignatures(1).build())
            .build();

    assertThat(onboardingRules.resolveWorkflowType(child, InstitutionType.PA, Origin.IPA, false))
        .contains(WorkflowType.CONTRACT_REGISTRATION);
  }

  @Test
  void resolveWorkflowType_usesMatchingWorkflowRule() {
    Product product =
        Product.builder()
            .parentId(" ")
            .workflowRules(
                List.of(
                    rule(InstitutionType.GSP, Origin.IPA, WorkflowType.FOR_APPROVE),
                    rule(InstitutionType.PA, Origin.SELC, WorkflowType.FOR_APPROVE),
                    rule(InstitutionType.PA, Origin.IPA, null),
                    rule(InstitutionType.PA, Origin.IPA, WorkflowType.CONTRACT_REGISTRATION)))
            .build();

    assertThat(onboardingRules.resolveWorkflowType(product, InstitutionType.PA, Origin.IPA, false))
        .contains(WorkflowType.CONTRACT_REGISTRATION);
  }

  @Test
  void resolveWorkflowType_isEmptyWithoutRules() {
    assertThat(
            onboardingRules.resolveWorkflowType(
                Product.builder().build(), InstitutionType.PA, Origin.IPA, false))
        .isEmpty();
  }

  @Test
  void isWorkflowAllowedForOrigin_checksConfiguredOrigins() {
    Product root = Product.builder().build();

    assertThat(onboardingRules.isWorkflowAllowedForOrigin(root, Origin.SELC, WorkflowType.IMPORT))
        .isTrue();
    assertThat(
            onboardingRules.isWorkflowAllowedForOrigin(
                root, Origin.SELC, WorkflowType.CONTRACT_REGISTRATION))
        .isFalse();
    assertThat(
            onboardingRules.isWorkflowAllowedForOrigin(
                root, Origin.IPA, WorkflowType.CONTRACT_REGISTRATION))
        .isTrue();
  }

  @Test
  void isWorkflowAllowedForOrigin_skipsProductsWithParent() {
    Product child = Product.builder().parentId("prod-parent").build();

    assertThat(
            onboardingRules.isWorkflowAllowedForOrigin(
                child, Origin.SELC, WorkflowType.CONTRACT_REGISTRATION))
        .isTrue();
  }

  @Test
  void validate_acceptsCoherentConfiguration() {
    Product product =
        Product.builder()
            .productId("prod-pagopa")
            .institutionOrigins(
                List.of(
                    entry(InstitutionType.PA, Origin.IPA, null),
                    entry(InstitutionType.GPU, Origin.SELC, false)))
            .workflowRules(
                List.of(
                    rule(InstitutionType.PA, Origin.IPA, WorkflowType.CONTRACT_REGISTRATION),
                    rule(InstitutionType.GPU, Origin.SELC, WorkflowType.FOR_APPROVE_GPU)))
            .build();

    assertThat(onboardingRules.validate(product)).isEmpty();
    assertThat(onboardingRules.validate(Product.builder().build())).isEmpty();
  }

  @Test
  void validate_reportsEveryOriginEntryProblem() {
    List<OriginEntry> origins = new ArrayList<>();
    origins.add(entry(null, Origin.IPA, null));
    origins.add(entry(InstitutionType.PA, null, null));
    origins.add(entry(InstitutionType.SA, Origin.IPA, false));
    origins.add(entry(InstitutionType.SA, Origin.IPA, false));
    origins.add(entry(InstitutionType.GSP, Origin.IPA, true));
    origins.add(entry(InstitutionType.GPU, Origin.SELC, null));
    Product product =
        Product.builder()
            .institutionOrigins(origins)
            .workflowRules(
                List.of(rule(InstitutionType.GPU, Origin.SELC, WorkflowType.CONTRACT_REGISTRATION)))
            .build();

    assertThat(onboardingRules.validate(product))
        .containsExactly(
            "institutionOrigins entry without institutionType or origin",
            "institutionOrigins entry without institutionType or origin",
            "institutionOrigins SA/IPA: InstitutionType 'SA' is not allowed for origin 'IPA'."
                + " Allowed institution types: [GSP, PA, SCEC]",
            "institutionOrigins SA/IPA is duplicated",
            "institutionOrigins SA/IPA: InstitutionType 'SA' is not allowed for origin 'IPA'."
                + " Allowed institution types: [GSP, PA, SCEC]",
            "institutionOrigins GSP/IPA is self-onboardable but has no workflow",
            "institutionOrigins GPU/SELC resolves workflow CONTRACT_REGISTRATION not allowed for"
                + " origin SELC");
  }

  @Test
  void validate_reportsEveryWorkflowRuleProblem() {
    List<WorkflowRule> rules = new ArrayList<>();
    rules.add(rule(null, Origin.IPA, WorkflowType.FOR_APPROVE));
    rules.add(rule(InstitutionType.PA, null, WorkflowType.FOR_APPROVE));
    rules.add(rule(InstitutionType.PA, Origin.IPA, null));
    rules.add(rule(InstitutionType.PA, Origin.IPA, WorkflowType.CONTRACT_REGISTRATION));
    rules.add(rule(InstitutionType.PA, Origin.IPA, WorkflowType.FOR_APPROVE));
    rules.add(rule(InstitutionType.GSP, Origin.IPA, WorkflowType.FOR_APPROVE));
    Product product =
        Product.builder()
            .institutionOrigins(List.of(entry(InstitutionType.PA, Origin.IPA, null)))
            .workflowRules(rules)
            .build();

    assertThat(onboardingRules.validate(product))
        .containsExactly(
            "workflowRules entry without institutionType, origin or workflowType",
            "workflowRules entry without institutionType, origin or workflowType",
            "workflowRules entry without institutionType, origin or workflowType",
            "workflowRules PA/IPA is duplicated",
            "workflowRules GSP/IPA has no institutionOrigins entry");
  }

  @Test
  void enforce_doesNothingOnCoherentConfiguration() {
    assertThatCode(() -> strictRules().enforce(Product.builder().build()))
        .doesNotThrowAnyException();
  }

  @Test
  void enforce_onlyLogsInWarnMode() {
    Product product =
        Product.builder()
            .productId("prod-test")
            .institutionOrigins(List.of(entry(InstitutionType.PA, Origin.IPA, null)))
            .build();

    assertThatCode(() -> rules(ValidationMode.WARN).enforce(product)).doesNotThrowAnyException();
  }

  @Test
  void enforce_rejectsInStrictMode() {
    Product product =
        Product.builder()
            .productId("prod-test")
            .institutionOrigins(List.of(entry(InstitutionType.PA, Origin.IPA, null)))
            .build();

    assertThatThrownBy(() -> strictRules().enforce(product))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(
            "Invalid onboarding rules for product prod-test: institutionOrigins PA/IPA is"
                + " self-onboardable but has no workflow");
  }
}
