package it.pagopa.selfcare.dashboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GeographicTaxonomyListDto {
    @Schema(description = "${swagger.dashboard.geographicTaxonomyList.model.geographicTaxonomyDtoList}")
    @JsonProperty(required = true)
    @NotNull
    private List<GeographicTaxonomyDto> geographicTaxonomyDtoList;
}
