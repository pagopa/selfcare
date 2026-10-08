package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.common.PartyRole;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.model.UserAuthority;
import it.pagopa.selfcare.onboarding.client.model.*;
import it.pagopa.selfcare.onboarding.model.dto.response.*;
import org.openapi.quarkus.user_json.model.OnboardedProductResponse;
import org.openapi.quarkus.user_json.model.OnboardedProductState;
import org.openapi.quarkus.user_json.model.UserInstitutionResponse;
import org.openapi.quarkus.onboarding_json.model.GetInstitutionRequest;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "jakarta-cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InstitutionMapper {

    default OnboardingInstitutionRequest toOnboardingInstitutionRequest(OnboardingData onboardingData) {
        OnboardingInstitutionRequest request = new OnboardingInstitutionRequest();
        request.setInstitutionExternalId(onboardingData.getInstitutionExternalId());
        request.setPricingPlan(onboardingData.getPricingPlan());
        request.setBilling(onboardingData.getBilling());
        request.setProductId(onboardingData.getProductId());
        request.setProductName(onboardingData.getProductName());

        InstitutionUpdate source = onboardingData.getInstitutionUpdate();
        InstitutionUpdate institutionUpdate = new InstitutionUpdate();
        institutionUpdate.setInstitutionType(onboardingData.getInstitutionType());
        institutionUpdate.setAddress(source.getAddress());
        institutionUpdate.setDescription(source.getDescription());
        institutionUpdate.setDigitalAddress(source.getDigitalAddress());
        institutionUpdate.setTaxCode(source.getTaxCode());
        institutionUpdate.setZipCode(source.getZipCode());
        institutionUpdate.setPaymentServiceProvider(source.getPaymentServiceProvider());
        institutionUpdate.setDataProtectionOfficer(source.getDataProtectionOfficer());
        if (onboardingData.getLocation() != null) {
            institutionUpdate.setCity(onboardingData.getLocation().getCity());
            institutionUpdate.setCounty(onboardingData.getLocation().getCounty());
            institutionUpdate.setCountry(onboardingData.getLocation().getCountry());
        }
        if (Objects.nonNull(source.getGeographicTaxonomies())) {
            institutionUpdate.setGeographicTaxonomyCodes(source.getGeographicTaxonomies().stream()
                    .map(GeographicTaxonomy::getCode).toList());
        }
        institutionUpdate.setRea(source.getRea());
        institutionUpdate.setShareCapital(source.getShareCapital());
        institutionUpdate.setBusinessRegisterPlace(source.getBusinessRegisterPlace());
        institutionUpdate.setSupportEmail(source.getSupportEmail());
        institutionUpdate.setSupportPhone(source.getSupportPhone());
        institutionUpdate.setImported(source.getImported());
        institutionUpdate.setAdditionalInformations(source.getAdditionalInformations());
        request.setInstitutionUpdate(institutionUpdate);

        request.setUsers(onboardingData.getUsers().stream()
                .map(userInfo -> {
                    User user = new User();
                    user.setId(userInfo.getId());
                    user.setName(userInfo.getName());
                    user.setSurname(userInfo.getSurname());
                    user.setTaxCode(userInfo.getTaxCode());
                    user.setEmail(userInfo.getEmail());
                    user.setRole(userInfo.getRole());
                    user.setProductRole(userInfo.getProductRole());
                    return user;
                }).toList());

        OnboardingContract contract = new OnboardingContract();
        contract.setPath(onboardingData.getContractPath());
        contract.setVersion(onboardingData.getContractVersion());
        request.setContract(contract);
        return request;
    }

    default InstitutionFromIpaPost toInstitutionFromIpaPost(String taxCode, String subunitCode, String subunitType) {
        InstitutionFromIpaPost request = new InstitutionFromIpaPost();
        request.setSubunitCode(subunitCode);
        request.setTaxCode(taxCode);
        request.setSubunitType(subunitType);
        return request;
    }

    default GetInstitutionRequest toGetInstitutionRequest(List<InstitutionInfo> institutions) {
        GetInstitutionRequest request = new GetInstitutionRequest();
        request.setInstitutionIds(institutions.stream().map(InstitutionInfo::getId).toList());
        return request;
    }

    default InstitutionOnboardingData toInstitutionOnboardingData(Institution institution, InstitutionInfo institutionInfo) {
        InstitutionOnboardingData result = new InstitutionOnboardingData();
        result.setInstitution(institutionInfo);
        result.setGeographicTaxonomies(institution.getGeographicTaxonomies());
        result.setCompanyInformations(institution.getCompanyInformations());
        result.setAssistanceContacts(institution.getAssistanceContacts());
        return result;
    }

    default void updateInstitutionInfo(Institution institution, InstitutionInfo institutionInfo) {
        InstitutionLocation institutionLocation = new InstitutionLocation();
        institutionLocation.setCountry(institution.getCountry());
        institutionLocation.setCity(institution.getCity());
        institutionLocation.setCounty(institution.getCounty());
        institutionInfo.setInstitutionLocation(institutionLocation);
        institutionInfo.setSubunitCode(institution.getSubunitCode());
        institutionInfo.setSubunitType(institution.getSubunitType());
        institutionInfo.setOrigin(institution.getOrigin());
    }

    @Mapping(target = "companyInformations", source = ".", qualifiedByName = "toCompanyInformationsEntity")
    @Mapping(target = "assistanceContacts", source = ".", qualifiedByName = "toAssistanceContacts")
    Institution toEntity(InstitutionResponse dto);

    @Named("toCompanyInformationsEntity")
    default CompanyInformations toCompanyInformationsEntity(InstitutionResponse dto) {
        CompanyInformations companyInformations = new CompanyInformations();
        companyInformations.setRea(dto.getRea());
        companyInformations.setShareCapital(dto.getShareCapital());
        companyInformations.setBusinessRegisterPlace(dto.getBusinessRegisterPlace());
        return companyInformations;
    }

    @Mapping(target = "id", source = "institutionId")
    InstitutionInfo toInstitutionInfo(BillingDataResponse model);

    @Named("toAssistanceContacts")
    default AssistanceContacts toAssistanceContacts(InstitutionResponse dto) {
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
    default PartyRole toPartyRole(UserInstitutionResponse model) {
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
    default String toStatus(UserInstitutionResponse model) {
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

    @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
    @Mapping(target = "userRole", source = "userRole", qualifiedByName = "toUserAuthority")
    InstitutionResource toResource(InstitutionInfo model);

    InstitutionResourceIC toResource(InstitutionInfoIC model);

    BusinessResourceIC toResource(BusinessInfoIC model);

    @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
    @Mapping(target = "institutionType", source = "institutionType", qualifiedByName = "enumToString")
    InstitutionResource toResource(Institution model);

    MatchInfoResultResource toResource(MatchInfoResult model);

    @Named("stringToUuid")
    default UUID stringToUuid(String id) {
        return id != null ? UUID.fromString(id) : null;
    }

    @Named("enumToString")
    default String enumToString(Enum<?> enumValue) {
        return enumValue != null ? enumValue.name() : null;
    }

    GeographicTaxonomyResource toResource(GeographicTaxonomy model);

    ProductResource toResource(Product model);

    OriginResponse toOriginResponse(OriginResult originEntries);

    @Mapping(target = "institution", expression = "java(toInstitutionData(model.getInstitution(), model.getAssistanceContacts(), model.getCompanyInformations()))")
    InstitutionOnboardingInfoResource toResource(InstitutionOnboardingData model);

    @Mapping(target = "billingData", source = "model", qualifiedByName = "toBilling")
    @Mapping(target = "city", source = "model.institutionLocation.city")
    @Mapping(target = "country", source = "model.institutionLocation.country")
    @Mapping(target = "county", source = "model.institutionLocation.county")
    @Mapping(target = "institutionType", source = "model.institutionType")
    InstitutionData toInstitutionData(InstitutionInfo model, AssistanceContacts assistanceContacts, CompanyInformations companyInformations);

    @Named("toBilling")
    @Mapping(target = "publicServices", source = "model.billing.publicServices")
    @Mapping(target = "recipientCode", source = "model.billing.recipientCode")
    @Mapping(target = "vatNumber", source = "model.billing.vatNumber")
    @Mapping(target = "registeredOffice", source = "address")
    @Mapping(target = "businessName", source = "description")
    BillingDataResponseDto toBilling(InstitutionInfo model);

    AssistanceContactsResource toResource(AssistanceContacts model);

    CompanyInformationsResource toResource(CompanyInformations model);

    @Named("toUserAuthority")
    default UserAuthority mapUserRole(PartyRole model) {
        if (model == null) {
            return null;
        }
        return switch (model) {
            case MANAGER, DELEGATE, SUB_DELEGATE -> UserAuthority.ADMIN;
            case OPERATOR -> UserAuthority.LIMITED;
            case ADMIN_EA -> UserAuthority.ADMIN_EA;
        };
    }

    default UUID mapId(String id) {
        return id != null ? UUID.fromString(id) : null;
    }

    InstitutionInfo toInstitutionInfo(Institution model);

    // Only the fields carried by the former InstitutionUpdate projection are returned: location and subunit are not
    default Institution toInstitution(org.openapi.quarkus.onboarding_json.model.InstitutionResponse model) {
        if (model == null) {
            return null;
        }
        Institution institution = new Institution();
        institution.setId(model.getId());
        institution.setDescription(model.getDescription());
        institution.setTaxCode(model.getTaxCode());
        institution.setDigitalAddress(model.getDigitalAddress());
        institution.setAddress(model.getAddress());
        institution.setZipCode(model.getZipCode());
        institution.setOrigin(model.getOrigin() != null ? model.getOrigin().name() : null);
        institution.setOriginId(model.getOriginId());
        institution.setParentDescription(model.getParentDescription());
        if (model.getInstitutionType() != null) {
            institution.setInstitutionType(InstitutionType.valueOf(model.getInstitutionType()));
        }
        return institution;
    }
}
