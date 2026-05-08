package it.pagopa.selfcare.dashboard.model.onboarding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
public class OnboardingRequestResource {


    @Schema(description = "${swagger.dashboard.onboarding-request.model.status}")
    @JsonProperty(required = true)
    private OnboardingStatus status;

    @Schema(description = "${swagger.dashboard.onboarding-request.model.institutionInfo}")
    @JsonProperty(required = true)
    private InstitutionInfo institutionInfo;

    @Schema(description = "${swagger.dashboard.onboarding-request.model.manager}")
    private UserInfo manager;

    @Schema(description = "${swagger.dashboard.onboarding-request.model.admins}")
    private List<UserInfo> admins;

    @Schema(description = "${swagger.dashboard.onboarding-request.model.productId}")
    private String productId;


    @Data
    @EqualsAndHashCode(of = "id")
    public static class InstitutionInfo {

        @Schema(description = "${swagger.dashboard.institutions.model.id}")
        private String id;

        @Schema(description = "${swagger.dashboard.institutions.model.name}")
        @JsonProperty(required = true)
        private String name;

        @Schema(description = "${swagger.dashboard.institutions.model.institutionType}")
        @JsonProperty(required = true)
        private InstitutionType institutionType;

        @Schema(description = "${swagger.dashboard.institutions.model.address}")
        @JsonProperty(required = true)
        private String address;

        @Schema(description = "${swagger.dashboard.institutions.model.zipCode}")
        @JsonProperty(required = true)
        private String zipCode;

        @Schema(description = "${swagger.dashboard.institutions.model.city}")
        private String city;

        @Schema(description = "${swagger.dashboard.institutions.model.country}")
        private String country;

        @Schema(description = "${swagger.dashboard.institutions.model.county}")
        private String county;

        @Schema(description = "${swagger.dashboard.institutions.model.mailAddress}")
        @JsonProperty(required = true)
        private String mailAddress;

        @Schema(description = "${swagger.dashboard.institutions.model.fiscalCode}")
        @JsonProperty(required = true)
        private String fiscalCode;

        @Schema(description = "${swagger.dashboard.institutions.model.vatNumber}")
        @JsonProperty(required = true)
        private String vatNumber;

        @Schema(description = "${swagger.dashboard.institutions.model.recipientCode}")
        private String recipientCode;

        @Schema(description = "${swagger.dashboard.institutions.model.pspData}")
        private PspData pspData;

        @Schema(description = "${swagger.dashboard.institutions.model.dpoData}")
        private DpoData dpoData;

        @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations}")
        private AdditionalInformations additionalInformations;

        @Data
        public static class AdditionalInformations{
            @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations.belongRegulatedMarket}")
            private boolean belongRegulatedMarket;

            @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations.regulatedMarketNote}")
            private String regulatedMarketNote;

            @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations.ipa}")
            private boolean ipa;

            @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations.ipaCode}")
            private String ipaCode;

            @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations.establishedByRegulatoryProvision}")
            private boolean establishedByRegulatoryProvision;

            @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations.establishedByRegulatoryProvisionNote}")
            private String establishedByRegulatoryProvisionNote;

            @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations.agentOfPublicService}")
            private boolean agentOfPublicService;

            @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations.agentOfPublicServiceNote}")
            private String agentOfPublicServiceNote;

            @Schema(description = "${swagger.dashboard.institutions.model.additionalInformations.otherNote}")
            private String otherNote;
        }

        @Data
        public static class PspData {

            @Schema(description = "${swagger.dashboard.institutions.model.pspData.businessRegisterNumber}")
            @JsonProperty(required = true)
            private String businessRegisterNumber;

            @Schema(description = "${swagger.dashboard.institutions.model.pspData.legalRegisterName}")
            @JsonProperty(required = true)
            private String legalRegisterName;

            @Schema(description = "${swagger.dashboard.institutions.model.pspData.legalRegisterNumber}")
            @JsonProperty(required = true)
            private String legalRegisterNumber;

            @Schema(description = "${swagger.dashboard.institutions.model.pspData.abiCode}")
            @JsonProperty(required = true)
            private String abiCode;

            @Schema(description = "${swagger.dashboard.institutions.model.pspData.vatNumberGroup}")
            @JsonProperty(required = true)
            private Boolean vatNumberGroup;
        }


        @Data
        public static class DpoData {

            @Schema(description = "${swagger.dashboard.institutions.model.dpoData.address}")
            @JsonProperty(required = true)
            @NotBlank
            private String address;

            @Schema(description = "${swagger.dashboard.institutions.model.dpoData.pec}")
            @JsonProperty(required = true)
            @NotBlank
            @Email
            private String pec;

            @Schema(description = "${swagger.dashboard.institutions.model.dpoData.email}")
            @JsonProperty(required = true)
            @NotBlank
            @Email
            private String email;
        }
    }

    @Data
    public static class UserInfo {

        @Schema(description = "${swagger.dashboard.user.model.id}")
        @JsonProperty(required = true)
        private UUID id;

        @Schema(description = "${swagger.dashboard.user.model.name}")
        @JsonProperty(required = true)
        private String name;

        @Schema(description = "${swagger.dashboard.user.model.surname}")
        @JsonProperty(required = true)
        private String surname;

        @Schema(description = "${swagger.dashboard.user.model.institutionalEmail}")
        @JsonProperty(required = true)
        private String email;

        @Schema(description = "${swagger.dashboard.user.model.fiscalCode}")
        @JsonProperty(required = true)
        private String fiscalCode;

    }
}
