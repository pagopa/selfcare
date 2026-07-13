package it.pagopa.selfcare.document.model.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAttachmentRequest {

    @NotNull
    private String onboardingId;

    @NotNull
    private String productId;

    @NotNull
    private String attachmentName;

    @NotNull
    private String attachmentId;

    @NotNull
    private Integer maxDocumentsRequired;

    private String attachmentDescription;
}

