package it.pagopa.selfcare.onboarding.connector.rest.mapper;


import it.pagopa.selfcare.commons.base.security.PartyRole;
import it.pagopa.selfcare.onboarding.connector.model.institutions.*;
import it.pagopa.selfcare.onboarding.connector.rest.model.BillingDataResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.InstitutionResponse;
import it.pagopa.selfcare.onboarding.connector.rest.model.OnboardingResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.OnboardedProductState;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
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
                    .map(OnboardedProductState::getValue)
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
