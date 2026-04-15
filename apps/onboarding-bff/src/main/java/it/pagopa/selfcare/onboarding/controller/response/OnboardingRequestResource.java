package it.pagopa.selfcare.onboarding.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OnboardingRequestResource {


    @Schema(description = "${openapi.onboarding.model.status}", required = true)
    @JsonProperty(required = true)
    private String status;

    @Schema(description = "${openapi.onboarding.model.institutionInfo}", required = true)
    @JsonProperty(required = true)
    private InstitutionInfo institutionInfo;

    @Schema(description = "${openapi.onboarding.model.manager}")
    private UserInfo manager;

    @Schema(description = "${openapi.onboarding.model.admins}")
    private List<UserInfo> admins;

    @Schema(description = "${openapi.onboarding.product.model.id}")
    private String productId;

    @Schema(description = "${openapi.onboarding.model.updateDate}")
    private LocalDateTime updatedAt;

    @Schema(description = "${openapi.onboarding.model.expiringDate}")
    private LocalDateTime expiringDate;

    @Schema(description = "${openapi.onboarding.model.reason}")
    private String reasonForReject;

    @Schema(description = "${openapi.onboarding.model.attachments}")
    private List<String> attachments;

    @Data
    @EqualsAndHashCode(of = "id")
    public static class InstitutionInfo {

        @Schema(description = "${openapi.onboarding.institutions.model.id}")
        private String id;

        @Schema(description = "${openapi.onboarding.institutions.model.name}", required = true)
        @JsonProperty(required = true)
        private String name;

        @Schema(description = "${openapi.onboarding.institutions.model.institutionType}", required = true)
        @JsonProperty(required = true)
        private String institutionType;

        @Schema(description = "${openapi.onboarding.institutions.model.address}", required = true)
        @JsonProperty(required = true)
        private String address;

        @Schema(description = "${openapi.onboarding.institutions.model.zipCode}", required = true)
        @JsonProperty(required = true)
        private String zipCode;

        @Schema(description = "${openapi.onboarding.institutions.model.city}")
        private String city;

        @Schema(description = "${openapi.onboarding.institutions.model.country}")
        private String country;

        @Schema(description = "${openapi.onboarding.institutions.model.county}")
        private String county;

        @Schema(description = "${openapi.onboarding.institutions.model.digitalAddress}", required = true)
        @JsonProperty(required = true)
        private String mailAddress;

        @Schema(description = "${openapi.onboarding.institutions.model.taxCode}", required = true)
        @JsonProperty(required = true)
        private String fiscalCode;

        @Schema(description = "${openapi.onboarding.institutions.model.taxCodeInvoicing}")
        private String taxCodeInvoicing;

        @Schema(description = "${openapi.onboarding.institutions.model.vatNumber}", required = true)
        private String vatNumber;

        @Schema(description = "${openapi.onboarding.institutions.model.recipientCode}")
        private String recipientCode;

        @Schema(description = "${openapi.onboarding.institutions.model.pspData}")
        private PspData pspData;

        @Schema(description = "${openapi.onboarding.institutions.model.dpoData}")
        private DpoData dpoData;

        @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations}")
        private AdditionalInformations additionalInformations;

        @Data
        public static class AdditionalInformations{
            @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations.belongRegulatedMarket}")
            private boolean belongRegulatedMarket;

            @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations.regulatedMarketNote}")
            private String regulatedMarketNote;

            @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations.ipa}")
            private boolean ipa;

            @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations.ipaCode}")
            private String ipaCode;

            @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations.establishedByRegulatoryProvision}")
            private boolean establishedByRegulatoryProvision;

            @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations.establishedByRegulatoryProvisionNote}")
            private String establishedByRegulatoryProvisionNote;

            @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations.agentOfPublicService}")
            private boolean agentOfPublicService;

            @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations.agentOfPublicServiceNote}")
            private String agentOfPublicServiceNote;

            @Schema(description = "${openapi.onboarding.institutions.model.additionalInformations.otherNote}")
            private String otherNote;
        }

        @Data
        public static class PspData {

            @Schema(description = "${openapi.onboarding.institutions.model.pspData.businessRegisterNumber}", required = true)
            @JsonProperty(required = true)
            private String businessRegisterNumber;

            @Schema(description = "${openapi.onboarding.institutions.model.pspData.legalRegisterName}", required = true)
            @JsonProperty(required = true)
            private String legalRegisterName;

            @Schema(description = "${openapi.onboarding.institutions.model.pspData.legalRegisterNumber}", required = true)
            @JsonProperty(required = true)
            private String legalRegisterNumber;

            @Schema(description = "${openapi.onboarding.institutions.model.pspData.abiCode}", required = true)
            @JsonProperty(required = true)
            private String abiCode;

            @Schema(description = "${openapi.onboarding.institutions.model.pspData.vatNumberGroup}", required = true)
            @JsonProperty(required = true)
            private Boolean vatNumberGroup;
        }


        @Data
        public static class DpoData {

            @Schema(description = "${openapi.onboarding.institutions.model.dpoData.address}", required = true)
            @JsonProperty(required = true)
            @NotBlank
            private String address;

            @Schema(description = "${openapi.onboarding.institutions.model.dpoData.pec}", required = true)
            @JsonProperty(required = true)
            @NotBlank
            @Email
            private String pec;

            @Schema(description = "${openapi.onboarding.institutions.model.dpoData.email}", required = true)
            @JsonProperty(required = true)
            @NotBlank
            @Email
            private String email;
        }
    }

    @Data
    public static class UserInfo {

        @Schema(description = "${openapi.onboarding.user.model.id}", required = true)
        @JsonProperty(required = true)
        private UUID id;

        @Schema(description = "${openapi.onboarding.user.model.name}", required = true)
        @JsonProperty(required = true)
        private String name;

        @Schema(description = "${openapi.onboarding.user.model.surname}", required = true)
        @JsonProperty(required = true)
        private String surname;

        @Schema(description = "${openapi.onboarding.user.model.institutionalEmail}", required = true)
        @JsonProperty(required = true)
        private String email;

        @Schema(description = "${openapi.onboarding.user.model.fiscalCode}", required = true)
        @JsonProperty(required = true)
        private String fiscalCode;

    }
}
