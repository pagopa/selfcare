package it.pagopa.selfcare.onboarding.connector.model.institutions;

import com.fasterxml.jackson.annotation.JsonInclude;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.Billing;
import it.pagopa.selfcare.onboarding.connector.model.onboarding.GeographicTaxonomy;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class InstitutionsSearchResponse {
    private List<InstitutionDetails> institutions;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InstitutionDetails {
        @NotBlank
        private String id;
        private String externalId;
        @NotBlank
        private String origin;
        @NotBlank
        private String originId;
        private String description;
        private String institutionType;
        private String digitalAddress;
        private String address;
        private String zipCode;
        private String taxCode;
        private String city;
        private String county;
        private String country;
        private String istatCode;
        private List<GeographicTaxonomy> geographicTaxonomies;
        private List<Attribute> attributes;
        private List<OnboardingDetails> onboarding;
        private PaymentServiceProviderDetails paymentServiceProvider;
        private DataProtectionOfficerDetails dataProtectionOfficer;
        private RootParentDetails rootParent;
        private String rea;
        private String shareCapital;
        private String businessRegisterPlace;
        private String supportEmail;
        private String supportPhone;
        private boolean imported;
        private String subunitCode;
        private String subunitType;
        private String aooParentCode;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private boolean delegation;
        private String logo;
        private Boolean isTest;
        private String legalForm;
    }

    @Data
    public static class OnboardingDetails {
        private String productId;
        private String tokenId;
        private String status;
        private Billing billing;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private Boolean isAggregator;
        private String institutionType;
        private String origin;
        private String originId;
        private Boolean isPartnerTech;
    }

    @Data
    public static class PaymentServiceProviderDetails {
        private String abiCode;
        private String businessRegisterNumber;
        private String legalRegisterNumber;
        private String legalRegisterName;
        private boolean vatNumberGroup;
    }

    @Data
    public static class DataProtectionOfficerDetails {
        private String address;
        private String email;
        private String pec;
    }

    @Data
    public static class RootParentDetails {
        private String description;
        private String id;
    }
}
