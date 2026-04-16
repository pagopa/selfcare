package it.pagopa.selfcare.document.model.dto.request;

import it.pagopa.selfcare.onboarding.common.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentBuilderRequest {

    @NotNull
    private String onboardingId;

    @NotNull
    private String productId;

    @NotNull
    private DocumentType documentType;

    private String attachmentName;

    private String rootOnboardingId;

    /**
     * Template path (contract or attachment template).
     * Built by the calling MS.
     */
    private String templatePath;

    private String templateVersion;

    private List<String> fiscalCodes;

    /**
     * Product title - used for INSTITUTION and USER token types.
     */
    private String productTitle;

    /**
     * Checks if this is an attachment request.
     */
    public boolean isAttachment() {
        return DocumentType.ATTACHMENT.equals(documentType);
    }
}
