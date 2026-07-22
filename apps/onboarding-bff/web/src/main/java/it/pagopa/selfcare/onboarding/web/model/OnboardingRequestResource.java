package it.pagopa.selfcare.onboarding.web.model;


import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OnboardingRequestResource {


    @Schema(description = "${swagger.onboarding.model.status}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    private String status;

    @Schema(description = "${swagger.onboarding.model.institutionInfo}", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty(required = true)
    private InstitutionInfo institutionInfo;

    @Schema(description = "${swagger.onboarding.model.manager}")
    private UserInfo manager;

    @Schema(description = "${swagger.onboarding.model.admins}")
    private List<UserInfo> admins;

    @Schema(description = "${swagger.onboarding.product.model.id}")
    private String productId;

    @Schema(description = "${swagger.onboarding.model.updateDate}")
    private LocalDateTime updatedAt;

    @Schema(description = "${swagger.onboarding.model.expiringDate}")
    private LocalDateTime expiringDate;

    @Schema(description = "${swagger.onboarding.model.reason}")
    private String reasonForReject;

    @Schema(description = "${swagger.onboarding.model.attachments}")
    private List<String> attachments;

    @Data
    @EqualsAndHashCode(of = "id")
    public static class InstitutionInfo {

        @Schema(description = "${swagger.onboarding.institutions.model.id}")
        private String id;

        @Schema(description = "${swagger.onboarding.institutions.model.name}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String name;

        @Schema(description = "${swagger.onboarding.institutions.model.institutionType}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String institutionType;

        @Schema(description = "${swagger.onboarding.institutions.model.address}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String address;

        @Schema(description = "${swagger.onboarding.institutions.model.zipCode}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String zipCode;

        @Schema(description = "${swagger.onboarding.institutions.model.city}")
        private String city;

        @Schema(description = "${swagger.onboarding.institutions.model.country}")
        private String country;

        @Schema(description = "${swagger.onboarding.institutions.model.county}")
        private String county;

        @Schema(description = "${swagger.onboarding.institutions.model.digitalAddress}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String mailAddress;

        @Schema(description = "${swagger.onboarding.institutions.model.taxCode}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String fiscalCode;

        @Schema(description = "${swagger.onboarding.institutions.model.taxCodeInvoicing}")
        private String taxCodeInvoicing;

        @Schema(description = "${swagger.onboarding.institutions.model.vatNumber}", requiredMode = Schema.RequiredMode.REQUIRED)
        private String vatNumber;

        @Schema(description = "${swagger.onboarding.institutions.model.recipientCode}")
        private String recipientCode;

        @Schema(description = "${swagger.onboarding.institutions.model.pspData}")
        private PspData pspData;

        @Schema(description = "${swagger.onboarding.institutions.model.dpoData}")
        private DpoData dpoData;

        @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations}")
        private AdditionalInformations additionalInformations;

        @Data
        public static class AdditionalInformations{
            @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations.belongRegulatedMarket}")
            private boolean belongRegulatedMarket;

            @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations.regulatedMarketNote}")
            private String regulatedMarketNote;

            @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations.ipa}")
            private boolean ipa;

            @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations.ipaCode}")
            private String ipaCode;

            @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations.establishedByRegulatoryProvision}")
            private boolean establishedByRegulatoryProvision;

            @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations.establishedByRegulatoryProvisionNote}")
            private String establishedByRegulatoryProvisionNote;

            @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations.agentOfPublicService}")
            private boolean agentOfPublicService;

            @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations.agentOfPublicServiceNote}")
            private String agentOfPublicServiceNote;

            @Schema(description = "${swagger.onboarding.institutions.model.additionalInformations.otherNote}")
            private String otherNote;
        }

        @Data
        public static class PspData {

            @Schema(description = "${swagger.onboarding.institutions.model.pspData.businessRegisterNumber}", requiredMode = Schema.RequiredMode.REQUIRED)
            @JsonProperty(required = true)
            private String businessRegisterNumber;

            @Schema(description = "${swagger.onboarding.institutions.model.pspData.legalRegisterName}", requiredMode = Schema.RequiredMode.REQUIRED)
            @JsonProperty(required = true)
            private String legalRegisterName;

            @Schema(description = "${swagger.onboarding.institutions.model.pspData.legalRegisterNumber}", requiredMode = Schema.RequiredMode.REQUIRED)
            @JsonProperty(required = true)
            private String legalRegisterNumber;

            @Schema(description = "${swagger.onboarding.institutions.model.pspData.abiCode}", requiredMode = Schema.RequiredMode.REQUIRED)
            @JsonProperty(required = true)
            private String abiCode;

            @Schema(description = "${swagger.onboarding.institutions.model.pspData.vatNumberGroup}", requiredMode = Schema.RequiredMode.REQUIRED)
            @JsonProperty(required = true)
            private Boolean vatNumberGroup;
        }


        @Data
        public static class DpoData {

            @Schema(description = "${swagger.onboarding.institutions.model.dpoData.address}", requiredMode = Schema.RequiredMode.REQUIRED)
            @JsonProperty(required = true)
            @NotBlank
            private String address;

            @Schema(description = "${swagger.onboarding.institutions.model.dpoData.pec}", requiredMode = Schema.RequiredMode.REQUIRED)
            @JsonProperty(required = true)
            @NotBlank
            @Email
            private String pec;

            @Schema(description = "${swagger.onboarding.institutions.model.dpoData.email}", requiredMode = Schema.RequiredMode.REQUIRED)
            @JsonProperty(required = true)
            @NotBlank
            @Email
            private String email;
        }
    }

    @Data
    public static class UserInfo {

        @Schema(description = "${swagger.onboarding.user.model.id}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private UUID id;

        @Schema(description = "${swagger.onboarding.user.model.name}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String name;

        @Schema(description = "${swagger.onboarding.user.model.surname}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String surname;

        @Schema(description = "${swagger.onboarding.user.model.institutionalEmail}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String email;

        @Schema(description = "${swagger.onboarding.user.model.fiscalCode}", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty(required = true)
        private String fiscalCode;

    }
}
