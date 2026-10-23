package it.pagopa.selfcare.onboarding.service.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InstitutionTypeNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.onboarding.exception.OnboardingNotAllowedException;
import it.pagopa.selfcare.onboarding.exception.ResourceNotFoundException;
import it.pagopa.selfcare.onboarding.service.ProductService;
import jakarta.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.ArgumentCaptor;
import org.openapi.quarkus.product_json.model.OnboardingChannel;
import org.openapi.quarkus.product_json.model.OnboardingDecisionReason;
import org.openapi.quarkus.product_json.model.OnboardingDecisionRequest;
import org.openapi.quarkus.product_json.model.OnboardingDecisionResponse;
import org.openapi.quarkus.product_json.model.ProductResponse;

class OnboardingAdmissionHelperTest {

    private static final String PRODUCT_ID = "prod-pagopa";
    private static final String TENANT_ID = "AR";

    private final List<OnboardingDecisionResponse> applied = new ArrayList<>();
    private final Consumer<OnboardingDecisionResponse> onAdmitted = applied::add;
    private ProductService productService;
    private OnboardingAdmissionHelper helper;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        helper = new OnboardingAdmissionHelper();
        helper.productService = productService;
    }

    @Test
    void admit_shouldSendSelfServiceRequestAndApplyResolvedWorkflow() {
        Onboarding onboarding = onboarding(InstitutionType.PA, Origin.IPA, null);
        OnboardingDecisionResponse decision = allowed(org.openapi.quarkus.product_json.model.WorkflowType.CONTRACT_REGISTRATION);
        OnboardingDecisionRequest expected = new OnboardingDecisionRequest()
                .institutionType(org.openapi.quarkus.product_json.model.InstitutionType.PA)
                .origin(org.openapi.quarkus.product_json.model.Origin.IPA)
                .channel(OnboardingChannel.SELF_SERVICE)
                .aggregator(false);
        when(productService.getOnboardingDecision(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(decision));

        OnboardingAdmission admission = await(helper.admit(onboarding, OnboardingChannel.SELF_SERVICE, null, onAdmitted));

        assertRequest(expected);
        assertEquals(WorkflowType.CONTRACT_REGISTRATION, onboarding.getWorkflowType());
        assertEquals(List.of(decision), applied);
        assertEquals(OnboardingChannel.SELF_SERVICE, admission.channel());
        assertNull(admission.workflowType());
        assertEquals(InstitutionType.PA, admission.institutionType());
        assertSame(decision, admission.decision());
        assertSame(decision.getProduct(), admission.product());
    }

    @Test
    void admit_shouldSendImposedWorkflowAggregatorAndNormalizedOrigin() {
        Onboarding onboarding = onboarding(InstitutionType.GPU, Origin.MOCK, true);
        OnboardingDecisionResponse decision = allowed(org.openapi.quarkus.product_json.model.WorkflowType.CONFIRMATION);
        OnboardingDecisionRequest expected = new OnboardingDecisionRequest()
                .institutionType(org.openapi.quarkus.product_json.model.InstitutionType.GPU)
                .origin(org.openapi.quarkus.product_json.model.Origin.SELC)
                .channel(OnboardingChannel.MANAGED)
                .aggregator(true)
                .workflowType(org.openapi.quarkus.product_json.model.WorkflowType.CONFIRMATION);
        when(productService.getOnboardingDecision(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(decision));

        OnboardingAdmission admission = await(helper.admit(onboarding, OnboardingChannel.MANAGED,
                WorkflowType.CONFIRMATION, onAdmitted));

        assertRequest(expected);
        assertEquals(WorkflowType.CONFIRMATION, onboarding.getWorkflowType());
        assertEquals(WorkflowType.CONFIRMATION, admission.workflowType());
        assertEquals(List.of(decision), applied);
    }

    @Test
    void admit_shouldFailWithoutCallingProductMs_whenInstitutionTypeIsMissing() {
        Onboarding onboarding = onboarding(null, Origin.SELC, null);

        Throwable failure = failure(helper.admit(onboarding, OnboardingChannel.SELF_SERVICE, null, onAdmitted));

        assertInstanceOf(InstitutionTypeNotAllowedException.class, failure);
        assertEquals("institution with institution type null and origin SELC is not allowed to onboard product "
                + PRODUCT_ID, failure.getMessage());
        verifyNoInteractions(productService);
    }

    @ParameterizedTest
    @EnumSource(value = OnboardingDecisionReason.class,
            names = {"INSTITUTION_TYPE_NOT_ALLOWED", "SELF_ONBOARDING_NOT_ALLOWED"})
    void admit_shouldFailWithInstitutionTypeNotAllowed_whenThePairIsDenied(OnboardingDecisionReason reason) {
        Throwable failure = deniedFailure(reason, Boolean.FALSE);

        assertInstanceOf(InstitutionTypeNotAllowedException.class, failure);
        assertEquals(InstitutionTypeNotAllowedException.CODE, ((InvalidRequestException) failure).getCode());
        assertEquals("denied", failure.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidRequestDenials")
    void admit_shouldFailWithInvalidRequest_whenTheRequestIsDenied(OnboardingDecisionReason reason, Boolean allowed) {
        Throwable failure = deniedFailure(reason, allowed);

        assertEquals(InvalidRequestException.class, failure.getClass());
        assertEquals("denied", failure.getMessage());
    }

    static Stream<Arguments> invalidRequestDenials() {
        return Stream.of(
                Arguments.of(OnboardingDecisionReason.ORIGIN_INSTITUTION_TYPE_MISMATCH, Boolean.FALSE),
                Arguments.of(OnboardingDecisionReason.WORKFLOW_NOT_RESOLVABLE, Boolean.FALSE),
                Arguments.of(OnboardingDecisionReason.WORKFLOW_NOT_ALLOWED_FOR_ORIGIN, Boolean.FALSE),
                Arguments.of(null, null));
    }

    @Test
    void admit_shouldMapMissingProductToOnboardingNotAllowed() {
        Onboarding onboarding = onboarding(InstitutionType.PA, Origin.IPA, null);
        when(productService.getOnboardingDecision(any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("missing")));

        Throwable failure = failure(helper.admit(onboarding, OnboardingChannel.MANAGED, WorkflowType.IMPORT, onAdmitted));

        assertInstanceOf(OnboardingNotAllowedException.class, failure);
        assertEquals("Unable to complete the onboarding for institution with taxCode 'taxCode' to product '"
                + PRODUCT_ID + "', the product is dismissed.", failure.getMessage());
        assertEquals(List.of(), applied);
    }

    @Test
    void admit_shouldPropagateOtherProductMsFailures() {
        Onboarding onboarding = onboarding(InstitutionType.PA, Origin.IPA, null);
        WebApplicationException unavailable = new WebApplicationException(503);
        when(productService.getOnboardingDecision(any(), any(), any()))
                .thenReturn(Uni.createFrom().failure(unavailable));

        assertSame(unavailable, failure(helper.admit(onboarding, OnboardingChannel.SELF_SERVICE, null, onAdmitted)));
    }

    @Test
    void confirm_shouldKeepTheAdmission_whenInstitutionTypeIsUnchanged() {
        Onboarding onboarding = onboarding(InstitutionType.PA, Origin.IPA, null);
        OnboardingAdmission admission = new OnboardingAdmission(OnboardingChannel.MANAGED, WorkflowType.IMPORT,
                InstitutionType.PA, onAdmitted, allowed(org.openapi.quarkus.product_json.model.WorkflowType.IMPORT));

        assertSame(admission, await(helper.confirm(onboarding, admission)));
        verifyNoInteractions(productService);
    }

    @Test
    void confirm_shouldDecideAgainWithTheSameFlow_whenRegistryChangedInstitutionType() {
        Onboarding onboarding = onboarding(InstitutionType.GSP, Origin.IPA, null);
        OnboardingAdmission admission = new OnboardingAdmission(OnboardingChannel.MANAGED, WorkflowType.IMPORT,
                InstitutionType.PA, onAdmitted, allowed(org.openapi.quarkus.product_json.model.WorkflowType.IMPORT));
        OnboardingDecisionResponse decision = allowed(org.openapi.quarkus.product_json.model.WorkflowType.IMPORT);
        OnboardingDecisionRequest expected = new OnboardingDecisionRequest()
                .institutionType(org.openapi.quarkus.product_json.model.InstitutionType.GSP)
                .origin(org.openapi.quarkus.product_json.model.Origin.IPA)
                .channel(OnboardingChannel.MANAGED)
                .aggregator(false)
                .workflowType(org.openapi.quarkus.product_json.model.WorkflowType.IMPORT);
        when(productService.getOnboardingDecision(any(), any(), any()))
                .thenReturn(Uni.createFrom().item(decision));

        OnboardingAdmission confirmed = await(helper.confirm(onboarding, admission));

        assertEquals(InstitutionType.GSP, confirmed.institutionType());
        assertSame(decision, confirmed.decision());
        assertEquals(List.of(decision), applied);
        assertRequest(expected);
    }

    @ParameterizedTest
    @NullSource
    void confirm_shouldFail_whenRegistryRemovedInstitutionType(InstitutionType changed) {
        Onboarding onboarding = onboarding(changed, Origin.SELC, null);
        OnboardingAdmission admission = new OnboardingAdmission(OnboardingChannel.SELF_SERVICE, null,
                InstitutionType.PA, onAdmitted, allowed(org.openapi.quarkus.product_json.model.WorkflowType.FOR_APPROVE));

        assertInstanceOf(InstitutionTypeNotAllowedException.class, failure(helper.confirm(onboarding, admission)));
        verify(productService, never()).getOnboardingDecision(any(), any(), any());
    }

    private void assertRequest(OnboardingDecisionRequest expected) {
        ArgumentCaptor<OnboardingDecisionRequest> captor = ArgumentCaptor.forClass(OnboardingDecisionRequest.class);
        verify(productService, times(1)).getOnboardingDecision(eq(PRODUCT_ID), captor.capture(), eq(TENANT_ID));
        assertEquals(expected.toString(), captor.getValue().toString());
    }

    private Throwable deniedFailure(OnboardingDecisionReason reason, Boolean allowed) {
        Onboarding onboarding = onboarding(InstitutionType.GPU, Origin.SELC, null);
        when(productService.getOnboardingDecision(any(), any(), any())).thenReturn(Uni.createFrom().item(
                new OnboardingDecisionResponse().allowed(allowed).reason(reason).message("denied")));

        Throwable failure = failure(helper.admit(onboarding, OnboardingChannel.SELF_SERVICE, null, onAdmitted));

        assertNull(onboarding.getWorkflowType());
        assertEquals(List.of(), applied);
        return failure;
    }

    private static OnboardingDecisionResponse allowed(org.openapi.quarkus.product_json.model.WorkflowType workflowType) {
        return new OnboardingDecisionResponse().allowed(true).workflowType(workflowType)
                .product(new ProductResponse().productId(PRODUCT_ID));
    }

    private static Onboarding onboarding(InstitutionType institutionType, Origin origin, Boolean aggregator) {
        Institution institution = new Institution();
        institution.setInstitutionType(institutionType);
        institution.setOrigin(origin);
        institution.setTaxCode("taxCode");
        Onboarding onboarding = new Onboarding();
        onboarding.setProductId(PRODUCT_ID);
        onboarding.setTenantId(TENANT_ID);
        onboarding.setIsAggregator(aggregator);
        onboarding.setInstitution(institution);
        return onboarding;
    }

    private static <T> T await(Uni<T> uni) {
        return uni.subscribe().withSubscriber(UniAssertSubscriber.create()).assertCompleted().getItem();
    }

    private static Throwable failure(Uni<?> uni) {
        return uni.subscribe().withSubscriber(UniAssertSubscriber.create()).assertFailed().getFailure();
    }
}
