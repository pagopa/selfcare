package it.pagopa.selfcare.document.controller.request;

import it.pagopa.selfcare.product.entity.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentImportRequest {

    @NotBlank
    private String onboardingId;

    @NotBlank
    private Product product;

    @NotBlank
    private String productId;

    @NotBlank
    private String institutionType;

    @NotNull
    private LocalDateTime contractCreatedAt;

    @NotBlank
    private String contractFilePath;

    private String contractFileName;

}
