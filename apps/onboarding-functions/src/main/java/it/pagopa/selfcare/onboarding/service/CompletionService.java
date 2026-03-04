package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import org.openapi.quarkus.core_json.model.DelegationResponse;

import java.util.List;

public interface CompletionService {

    String createInstitutionAndPersistInstitutionId(Onboarding onboarding);

    void sendMailRejection(ExecutionContext context, Onboarding onboarding);

    void persistOnboarding(Onboarding onboarding);

    void persistActivatedAt(Onboarding onboarding);

    void rejectOutdatedOnboardings(Onboarding onboarding);

    void sendCompletedEmail(OnboardingWorkflow onboardingWorkflow);

    void sendCompletedEmailAggregate(Onboarding onboarding);

    void persistUsers(Onboarding onboarding);

    String createDelegation(Onboarding onboarding);

    String createAggregateOnboardingRequest(OnboardingAggregateOrchestratorInput onboardingAggregateOrchestratorInput);

    void sendTestEmail(ExecutionContext context);

    String existsDelegation(OnboardingAggregateOrchestratorInput onboardingAggregateOrchestratorInput);

    String verifyOnboardingAggregate(OnboardingAggregateOrchestratorInput onboardingAggregateOrchestratorInput);

    List<DelegationResponse> retrieveAggregates(Onboarding onboarding);


}
