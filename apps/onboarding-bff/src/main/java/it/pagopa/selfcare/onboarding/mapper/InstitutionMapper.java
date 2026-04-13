package it.pagopa.selfcare.onboarding.mapper;


import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.client.model.*;
import it.pagopa.selfcare.onboarding.client.model.BillingDataResponse;
import it.pagopa.selfcare.onboarding.client.model.InstitutionResponse;
import it.pagopa.selfcare.onboarding.client.model.OnboardingResponse;
import org.openapi.quarkus.user_json.model.OnboardedProductResponse;
import org.openapi.quarkus.user_json.model.OnboardedProductState;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "jakarta-cdi")
public interface InstitutionMapper {

    @Mapping(target = "companyInformations", source = ".", qualifiedByName = "toCompanyInformationsEntity")
    @Mapping(target = "assistanceContacts", source = ".", qualifiedByName = "toAssistanceContacts")
    Institution toEntity(InstitutionResponse dto);

    @Named("toCompanyInformationsEntity")
    static CompanyInformations toCompanyInformationsEntity(InstitutionResponse dto) {

        CompanyInformations companyInformations = new CompanyInformations();
        companyInformations.setRea(dto.getRea());
        companyInformations.setShareCapital(dto.getShareCapital());
        companyInformations.setBusinessRegisterPlace(dto.getBusinessRegisterPlace());
        return companyInformations;
    }

    @Mapping(target = "id", source = "institutionId")
    InstitutionInfo toInstitutionInfo(BillingDataResponse model);

    @Named("toAssistanceContacts")
    static AssistanceContacts toAssistanceContacts(InstitutionResponse dto) {

        AssistanceContacts assistanceContacts = new AssistanceContacts();
        assistanceContacts.setSupportEmail(dto.getSupportEmail());
        assistanceContacts.setSupportPhone(dto.getSupportPhone());
        return assistanceContacts;
    }

    OnboardingResource toResource(OnboardingResponse response);

    @Mapping(target = "id", source = "institutionId")
    @Mapping(target = "description", source = "institutionDescription")
    @Mapping(target = "userRole", source = ".", qualifiedByName = "toPartyRole")
    @Mapping(target = "status", source = ".", qualifiedByName = "toStatus")
    InstitutionInfo toInstitutionInfo(UserInstitutionResponse model);


    @Named("toPartyRole")
    static PartyRole toPartyRole(UserInstitutionResponse model) {
        try {
            return model.getProducts().stream()
                    .filter(product -> Objects.nonNull(product.getRole()))
                    .map(product -> PartyRole.valueOf(product.getRole().name()))
                    .reduce((role1,role2) -> Collections.min(List.of(role1, role2)))
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Named("toStatus")
    static String toStatus(UserInstitutionResponse model) {
        try {
            return model.getProducts().stream()
                    .filter(product -> Objects.nonNull(product.getRole()))
                    .reduce((product1,product2) -> product1.getRole().equals(Collections.min(List.of(product1.getRole(), product2.getRole())))
                        ? product1
                        : product2)
                    .map(OnboardedProductResponse::getStatus)
                    .map(OnboardedProductState::value)
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
