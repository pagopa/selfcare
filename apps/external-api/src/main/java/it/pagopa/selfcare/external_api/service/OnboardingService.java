package it.pagopa.selfcare.external_api.service;

import it.pagopa.selfcare.external_api.model.onboarding.OnboardingAggregatorImportData;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardingAggregatorImportDto;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardingData;
import it.pagopa.selfcare.external_api.model.onboarding.OnboardingUsersRequest;
import it.pagopa.selfcare.external_api.model.user.RelationshipInfo;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingAggregationImportRequest;
import java.util.List;

public interface OnboardingService {

  void autoApprovalOnboardingProductV2(OnboardingData onboardingData);

  void autoApprovalOnboardingImportProductV2(OnboardingData onboardingData);

  List<RelationshipInfo> onboardingUsers(
      OnboardingUsersRequest onboardingUsersRequest, String userName, String surname);

  OnboardingAggregatorImportData onboardingAggregatorImportBuildRequest(OnboardingAggregatorImportDto request, String taxCode);

  void aggregationImport(OnboardingAggregationImportRequest onboardingRequest);
}
