package it.pagopa.selfcare.onboarding.controller.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "${openapi.onboarding.institutions.model.companyInformations}")
public class CompanyInformationsDto {

    @Schema(description = "${openapi.onboarding.institutions.model.companyInformations.rea}")
    private String rea;

    @Schema(description = "${openapi.onboarding.institutions.model.companyInformations.shareCapital}")
    private String shareCapital;

    @Schema(description = "${openapi.onboarding.institutions.model.companyInformations.businessRegisterPlace}")
    private String businessRegisterPlace;

}
