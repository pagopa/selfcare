package it.pagopa.selfcare.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON payload used to pass managing institution and product information to activities.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManagingInstitutionGetEmailRequest {
    private String managingInstitutionId;
    private String productId;
    private String onboardingId;
}

