package it.pagopa.selfcare.document.model.dto.request;

import it.pagopa.selfcare.onboarding.common.TokenType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentBuilderRequest {

    @NotBlank
    private String onboardingId;

    @NotBlank
    private String productId;

    @NotNull
    private TokenType documentType;

    private String documentName;

    /**
     * Template path (contract or attachment template).
     * Built by the calling MS.
     */
    private String templatePath;

    private String templateVersion;

    private String pdfFormatFilename;

    /**
     * Product title - used for INSTITUTION and USER token types.
     */
    private String productTitle;

    /**
     * Checks if this is an attachment request.
     */
    public boolean isAttachment() {
        return TokenType.ATTACHMENT.equals(documentType);
    }
}
