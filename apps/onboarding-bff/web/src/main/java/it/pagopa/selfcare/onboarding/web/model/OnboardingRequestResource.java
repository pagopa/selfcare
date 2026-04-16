package it.pagopa.selfcare.onboarding.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OnboardingRequestResource {


    @ApiModelProperty(value = "${swagger.onboarding.model.status}", required = true)
    @JsonProperty(required = true)
    private String status;

    @ApiModelProperty(value = "${swagger.onboarding.model.institutionInfo}", required = true)
    @JsonProperty(required = true)
    private InstitutionInfo institutionInfo;

    @ApiModelProperty(value = "${swagger.onboarding.model.manager}")
    private UserInfo manager;

    @ApiModelProperty(value = "${swagger.onboarding.model.admins}")
    private List<UserInfo> admins;

    @ApiModelProperty(value = "${swagger.onboarding.product.model.id}")
    private String productId;

    @ApiModelProperty(value = "${swagger.onboarding.model.updateDate}")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "${swagger.onboarding.model.expiringDate}")
    private LocalDateTime expiringDate;

    @ApiModelProperty(value = "${swagger.onboarding.model.reason}")
    private String reasonForReject;

    @ApiModelProperty(value = "${swagger.onboarding.model.attachments}")
    private List<String> attachments;

    @Data
    @EqualsAndHashCode(of = "id")
    public static class InstitutionInfo {

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.id}")
        private String id;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.name}", required = true)
        @JsonProperty(required = true)
        private String name;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.institutionType}", required = true)
        @JsonProperty(required = true)
        private String institutionType;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.address}", required = true)
        @JsonProperty(required = true)
        private String address;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.zipCode}", required = true)
        @JsonProperty(required = true)
        private String zipCode;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.city}")
        private String city;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.country}")
        private String country;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.county}")
        private String county;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.digitalAddress}", required = true)
        @JsonProperty(required = true)
        private String mailAddress;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.taxCode}", required = true)
        @JsonProperty(required = true)
        private String fiscalCode;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.taxCodeInvoicing}")
        private String taxCodeInvoicing;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.vatNumber}", required = true)
        private String vatNumber;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.recipientCode}")
        private String recipientCode;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData}")
        private PspData pspData;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.dpoData}")
        private DpoData dpoData;

        @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations}")
        private AdditionalInformations additionalInformations;

        @Data
        public static class AdditionalInformations{
            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations.belongRegulatedMarket}")
            private boolean belongRegulatedMarket;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations.regulatedMarketNote}")
            private String regulatedMarketNote;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations.ipa}")
            private boolean ipa;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations.ipaCode}")
            private String ipaCode;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations.establishedByRegulatoryProvision}")
            private boolean establishedByRegulatoryProvision;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations.establishedByRegulatoryProvisionNote}")
            private String establishedByRegulatoryProvisionNote;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations.agentOfPublicService}")
            private boolean agentOfPublicService;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations.agentOfPublicServiceNote}")
            private String agentOfPublicServiceNote;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.additionalInformations.otherNote}")
            private String otherNote;
        }

        @Data
        public static class PspData {

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.businessRegisterNumber}", required = true)
            @JsonProperty(required = true)
            private String businessRegisterNumber;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.legalRegisterName}", required = true)
            @JsonProperty(required = true)
            private String legalRegisterName;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.legalRegisterNumber}", required = true)
            @JsonProperty(required = true)
            private String legalRegisterNumber;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.abiCode}", required = true)
            @JsonProperty(required = true)
            private String abiCode;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.pspData.vatNumberGroup}", required = true)
            @JsonProperty(required = true)
            private Boolean vatNumberGroup;
        }


        @Data
        public static class DpoData {

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.dpoData.address}", required = true)
            @JsonProperty(required = true)
            @NotBlank
            private String address;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.dpoData.pec}", required = true)
            @JsonProperty(required = true)
            @NotBlank
            @Email
            private String pec;

            @ApiModelProperty(value = "${swagger.onboarding.institutions.model.dpoData.email}", required = true)
            @JsonProperty(required = true)
            @NotBlank
            @Email
            private String email;
        }
    }

    @Data
    public static class UserInfo {

        @ApiModelProperty(value = "${swagger.onboarding.user.model.id}", required = true)
        @JsonProperty(required = true)
        private UUID id;

        @ApiModelProperty(value = "${swagger.onboarding.user.model.name}", required = true)
        @JsonProperty(required = true)
        private String name;

        @ApiModelProperty(value = "${swagger.onboarding.user.model.surname}", required = true)
        @JsonProperty(required = true)
        private String surname;

        @ApiModelProperty(value = "${swagger.onboarding.user.model.institutionalEmail}", required = true)
        @JsonProperty(required = true)
        private String email;

        @ApiModelProperty(value = "${swagger.onboarding.user.model.fiscalCode}", required = true)
        @JsonProperty(required = true)
        private String fiscalCode;

    }
}
