package it.pagopa.selfcare.onboarding.service;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.constants.CustomError;
import it.pagopa.selfcare.onboarding.controller.request.*;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGet;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingGetResponse;
import it.pagopa.selfcare.onboarding.controller.response.OnboardingResponse;
import it.pagopa.selfcare.onboarding.entity.CheckManagerResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.model.FormItem;
import it.pagopa.selfcare.onboarding.model.OnboardingGetFilters;

import java.util.List;

public interface OnboardingService {

    Uni<OnboardingResponse> onboarding(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates, UserRequesterDto userRequester);

    Uni<OnboardingResponse> onboardingUsers(
            OnboardingUserRequest onboardingUserRequest, String userId, WorkflowType workflowType);

    Uni<OnboardingResponse> onboardingImport(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            OnboardingImportContract contractImported,
            UserRequesterDto userRequester);

    Uni<OnboardingResponse> onboardingCompletion(
            Onboarding onboarding, List<UserRequest> userRequests, UserRequesterDto userRequester);

    Uni<OnboardingResponse> onboardingPgCompletion(
            Onboarding onboarding, List<UserRequest> userRequests);

    Uni<OnboardingResponse> onboardingAggregationCompletion(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates,
            UserRequesterDto userRequester);

    Uni<OnboardingResponse> onboardingAggregationImport(
            Onboarding onboarding,
            OnboardingImportContract contractImported,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates,
            UserRequesterDto userRequester);

    Uni<OnboardingResponse> onboardingUserPg(Onboarding onboarding, List<UserRequest> userRequests);

    Uni<OnboardingGet> approve(String onboardingId);

    Uni<Onboarding> complete(String tokenId, FormItem formItem);

    Uni<Onboarding> completeOnboardingUsers(String tokenId, FormItem formItem);

    Uni<Onboarding> completeWithoutSignatureVerification(String tokenId, FormItem formItem);

    Uni<Onboarding> uploadContractSigned(String tokenId, FormItem formItem);

    Uni<OnboardingGetResponse> onboardingGet(OnboardingGetFilters filters);

    Uni<Long> rejectOnboarding(String onboardingId, String reasonForReject);

    Uni<Long> deleteOnboarding(String onboardingId);

    Uni<OnboardingGet> onboardingPending(String onboardingId);

    Uni<List<OnboardingResponse>> institutionOnboardings(
            String taxCode, String subunitCode, String origin, String originId, OnboardingStatus status);

    Uni<List<OnboardingResponse>> verifyOnboarding(
            String taxCode,
            String subunitCode,
            String origin,
            String originId,
            OnboardingStatus status,
            String productId,
            InstitutionType institutionType);

    Uni<OnboardingGet> onboardingGet(String onboardingId);

    Uni<OnboardingGet> onboardingGetWithUserInfo(String onboardingId);

    Uni<Long> updateOnboarding(String onboardingId, Onboarding onboarding);

    Uni<CheckManagerResponse> checkManager(CheckManagerRequest checkManagerRequest);

    Uni<CustomError> checkRecipientCode(String recipientCode, String originId);

    Uni<OnboardingResponse> onboardingIncrement(
            Onboarding onboarding,
            List<UserRequest> userRequests,
            List<AggregateInstitutionRequest> aggregates,
            UserRequesterDto userRequester);

    Uni<OnboardingGet> retrieveOnboardingByInstitutionId(String institutionId, String productId);
}
