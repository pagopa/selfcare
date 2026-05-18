package it.pagopa.selfcare.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManagingInstitutionSendEmail {
    private String managingInstitutionId;
    private String productId;
    private String managingInstitutionDescription;
    private String userMailUuid;

}

