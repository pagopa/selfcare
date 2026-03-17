package it.pagopa.selfcare.document.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an attachment PDF document.
 * Contains all the data needed to generate the attachment without external calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAttachmentPdfRequest {

    @NotBlank
    private String onboardingId;

    @NotBlank
    private String attachmentTemplatePath;

    @NotBlank
    private String productId;

    @NotBlank
    private String productName;

    @NotBlank
    private String attachmentName;

    @NotNull
    @Valid
    private InstitutionPdfData institution;

    @NotNull
    @Valid
    private UserPdfData manager;
}
