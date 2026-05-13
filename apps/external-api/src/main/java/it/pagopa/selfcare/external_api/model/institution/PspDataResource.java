package it.pagopa.selfcare.external_api.model.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class PspDataResource {

    @Schema(description = "${swagger.external_api.institutions.model.pspData.businessRegisterNumber}")
    private String businessRegisterNumber;

    @Schema(description = "${swagger.external_api.institutions.model.pspData.legalRegisterName}")
    private String legalRegisterName;

    @Schema(description = "${swagger.external_api.institutions.model.pspData.legalRegisterNumber}")
    private String legalRegisterNumber;

    @Schema(description = "${swagger.external_api.institutions.model.pspData.abiCode}")
    private String abiCode;

    @Schema(description = "${swagger.external_api.institutions.model.pspData.vatNumberGroup}", example = "false")
    private Boolean vatNumberGroup;

}
