package it.pagopa.selfcare.onboarding.controller.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingImportContract {

    private String fileName;
    @NotEmpty(message = "filePath is required")
    private String filePath;
    private String contractType;
    @NotNull(message = "createdAt is required")
    private LocalDateTime createdAt;
    private LocalDateTime activatedAt;
}
