package it.pagopa.selfcare.document.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a contract PDF document.
 * Contains all the data needed to generate the contract without external calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractPdfRequest {

    @NotBlank
    private String onboardingId;

    @NotBlank
    private String contractTemplatePath;

    @NotBlank
    private String productId;

    @NotBlank
    private String productName;

    private String pricingPlan;

    private Boolean isAggregator;

    /**
     * Base URL for constructing aggregates CSV link
     */
    private String aggregatesCsvBaseUrl;

    @NotNull
    @Valid
    private InstitutionPdfData institution;

    @NotNull
    @Valid
    private UserPdfData manager;

    private List<@Valid UserPdfData> delegates;

    @Valid
    private BillingPdfData billing;

    @Valid
    private PaymentPdfData payment;
}
