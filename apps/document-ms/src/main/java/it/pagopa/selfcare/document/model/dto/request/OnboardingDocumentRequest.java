package it.pagopa.selfcare.document.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OnboardingDocumentRequest {

    @NotBlank
    private String onboardingId;

    @NotBlank
    private String productId;

    private LocalDateTime contractCreatedAt;

    @NotBlank
    private String contractFilePath;

    private String contractFileName;

    private String templatePath;

    private String templateVersion;

}
