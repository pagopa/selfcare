package it.pagopa.selfcare.external_api.model.institution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RootParentResource {
    @Schema(description = "${swagger.external_api.institutions.model.id}")
    private String id;
    @Schema(description = "swagger.external_api.institutions.model.parentDescription")
    private String description;
}
