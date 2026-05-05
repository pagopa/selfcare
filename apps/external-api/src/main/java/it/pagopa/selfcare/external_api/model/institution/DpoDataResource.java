package it.pagopa.selfcare.external_api.model.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class DpoDataResource {

    @Schema(description = "${swagger.external_api.institutions.model.pspData.dpoData.address}")
    private String address;

    @Schema(description = "${swagger.external_api.institutions.model.pspData.dpoData.pec}")
    private String pec;

    @Schema(description = "${swagger.external_api.institutions.model.pspData.dpoData.email}")
    private String email;

}
