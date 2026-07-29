package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CompanyInformationsDto {

    @Schema(description = "${swagger.onboarding.institutions.model.companyInformations.rea}")
    private String rea;

    @Schema(description = "${swagger.onboarding.institutions.model.companyInformations.shareCapital}")
    private String shareCapital;

    @Schema(description = "${swagger.onboarding.institutions.model.companyInformations.businessRegisterPlace}")
    private String businessRegisterPlace;

}
