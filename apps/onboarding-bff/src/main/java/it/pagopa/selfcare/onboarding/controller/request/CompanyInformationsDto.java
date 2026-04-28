package it.pagopa.selfcare.onboarding.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CompanyInformationsDto {

    @Schema(description = "${openapi.onboarding.institutions.model.companyInformations.rea}")
    private String rea;

    @Schema(description = "${openapi.onboarding.institutions.model.companyInformations.shareCapital}")
    private String shareCapital;

    @Schema(description = "${openapi.onboarding.institutions.model.companyInformations.businessRegisterPlace}")
    private String businessRegisterPlace;

}
