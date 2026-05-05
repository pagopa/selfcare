package it.pagopa.selfcare.external_api.model.pnpg;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PnPgInstitutionIdResource {
    @Schema(description = "${swagger.external_api.institutions.model.id}")
    private String id;
}
