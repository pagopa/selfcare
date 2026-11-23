package it.pagopa.selfcare.document.model.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.media.DiscriminatorMapping;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Request DTO for creating a contract PDF document.
 * Contains all the data needed to generate the contract without external calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "productId",
        visible = true,
        defaultImpl = ContractPdfRequest.class
)
@JsonSubTypes({@JsonSubTypes.Type(value = RtpContractPdfRequest.class, name = "prod-rtp")})
@Schema(
        discriminatorProperty = "productId",
        discriminatorMapping = @DiscriminatorMapping(value = "prod-rtp", schema = RtpContractPdfRequest.class))
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

}
