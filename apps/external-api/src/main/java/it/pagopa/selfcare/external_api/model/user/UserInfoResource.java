package it.pagopa.selfcare.external_api.model.user;

import io.swagger.v3.oas.annotations.media.Schema;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.external_api.model.onboarding.ProductInfo;
import lombok.Data;

import java.util.List;

;

@Data
public class UserInfoResource {

    @Schema(description = "${swagger.external_api.userInfo.model.user}")
    private UserResource user;

    @Schema(description = "${swagger.external_api.userInfo.model.onboardedInstitutions}")
    private List<OnboardedInstitutionResource> onboardedInstitutions;

    @Data
    public static class OnboardedInstitutionResource {

        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.id}")
        private String id;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.description}")
        private String description;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.institutionType}")
        private InstitutionType institutionType;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.origin}")
        private String origin;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.originId}")
        private String originId;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.digitalAddress}")
        private String digitalAddress;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.address}")
        private String address;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.state}")
        private String state;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.zipCode}")
        private String zipCode;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.taxCode}")
        private String taxCode;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.userEmail}")
        private String userEmail;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.userMobilePhone}")
        private String userMobilePhone;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.userTelephone}")
        private String userTelephone;
        @Schema(description = "${swagger.external_api.user.model.onboardedInstitutions.productInfo}")
        private ProductInfo productInfo;

    }

}
