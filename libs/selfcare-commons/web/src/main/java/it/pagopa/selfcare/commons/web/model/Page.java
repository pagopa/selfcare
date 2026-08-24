package it.pagopa.selfcare.commons.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class Page<T> {

    @Schema(description = "The page content")
    @JsonProperty(required = true)
    @NotNull
    @Valid
    private List<T> content;

    @Schema(description = "The total amount of elements")
    @JsonProperty(required = true)
    @NotNull
    private long totalElements;

    @Schema(description = "The number of total pages")
    @JsonProperty(required = true)
    @NotNull
    private int totalPages;

    @Schema(description = "The number of the current page")
    @JsonProperty(required = true)
    @NotNull
    private int number;

    @Schema(description = "The size of the page")
    @JsonProperty(required = true)
    @NotNull
    private int size;

}
