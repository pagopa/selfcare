package it.pagopa.selfcare.external_api.model.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class CompanyInformationsResource {

    @Schema(description = "${swagger.external_api.institutions.model.companyInformations.rea}")
    private String rea;

    @Schema(description = "${swagger.external_api.institutions.model.companyInformations.shareCapital}")
    private String shareCapital;

    @Schema(description = "${swagger.external_api.institutions.model.companyInformations.businessRegisterPlace}")
    private String businessRegisterPlace;

}
