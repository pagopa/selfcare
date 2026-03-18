package it.pagopa.selfcare.document.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for saving visura for merchant.
 * Contains all the data needed to generate the attachment without external calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadVisuraRequest {

    @NotBlank
    private String onboardingId;

    @NotBlank
    private String filename;

    @NotNull
    private byte[] fileContent;
}
