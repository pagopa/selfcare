package it.pagopa.selfcare.onboarding.service;

import com.microsoft.azure.functions.ExecutionContext;

public interface CheckOrganizationService {
    boolean checkOrganization(ExecutionContext context, String fiscalCode, String vatNumber);
    String testToken(ExecutionContext context);
}
