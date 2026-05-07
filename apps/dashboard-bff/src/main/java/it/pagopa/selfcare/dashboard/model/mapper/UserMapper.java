package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.dashboard.model.InstitutionUserDetailsResource;
import it.pagopa.selfcare.dashboard.model.InstitutionUserResource;
import it.pagopa.selfcare.dashboard.model.product.ProductInfoResource;
import it.pagopa.selfcare.dashboard.model.product.ProductRoleInfoResource;
import it.pagopa.selfcare.dashboard.model.product.ProductUserResource;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.model.user.*;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "familyName", source = "surname")
    it.pagopa.selfcare.user.generated.openapi.v1.dto.UpdateUserRequest toUpdateUserRequest(UpdateUserRequestDto userDto);

    @Named("toCertifiedField")
    default CertifiedField<String> toCertifiedField(CertifiableFieldResponseString field){
        if(Objects.nonNull(field) && Objects.nonNull(field.getCertified())){
            return new CertifiedField<>(Certification.valueOf(field.getCertified().name()), field.getValue());
        }
        return null;
    }

    @Mapping(target = "name",  expression = "java(toCertifiedField(response.getName()))")
    @Mapping(target = "familyName",  expression = "java(toCertifiedField(response.getFamilyName()))")
    @Mapping(target = "email",  expression = "java(toCertifiedField(response.getEmail()))")
    @Mapping(target = "workContacts", expression = "java(toCertifiedWorkContact(response.getWorkContacts()))")
    @Mapping(target = "mobilePhone",  expression = "java(toCertifiedField(response.getMobilePhone()))")
    User toUser(UserDetailResponse response);

    @Mapping(target = "id", source = "userId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "institutionId", source = "institutionId")
    @Mapping(target = "user.id", source = "userResponse.id")
    @Mapping(target = "user.fiscalCode", source = "userResponse.taxCode")
    @Mapping(target = "user.name", expression = "java(toCertifiedField(userResponse.getName()))")
    @Mapping(target = "user.familyName", expression = "java(toCertifiedField(userResponse.getSurname()))")
    @Mapping(target = "user.email", expression = "java(toCertifiedField(userResponse.getEmail()))")
    @Mapping(target = "user.workContacts", expression = "java(toUserInfoContacts(userResponse.getWorkContacts()))")
    @Mapping(target = "role", expression = "java(it.pagopa.selfcare.commons.base.security.PartyRole.valueOf(userDashboardResponse.getRole()).getSelfCareAuthority())")
    @Mapping(target = "products", expression = "java(toProductInfoMap(userDashboardResponse.getProducts()))")
    @Mapping(target = "user.mobilePhone", expression = "java(toCertifiedField(userResponse.getMobilePhone()))")
    UserInfo toUserInfo(UserDataResponse userDashboardResponse);

    @Mapping(target = "fiscalCode", source = "user.taxCode")
    @Mapping(target = "partyRole", expression = "java(mapPartyRole(latest))")
    @Mapping(target = "status", expression = "java(mapStatus(latest))")
    UserInstitutionRole toUserInstitutionRole(UserProductResponse user, OnboardedProductResponse latest);

    UserInstitutionWithActionsDto toUserInstitutionWithActionsDto(UserInstitutionWithActions userInstitutionWithActions);

    @Named("toUserInfoContacts")
    default Map<String, WorkContact> toUserInfoContacts(Map<String, String> workContactMap) {
        Map<String, WorkContact> resourceMap = new HashMap<>();
        if (workContactMap != null && !workContactMap.isEmpty()) {
            workContactMap.forEach((key, value) -> {
                WorkContact workContact = new WorkContact();
                workContact.setEmail(toCertifiedField(value));
                resourceMap.put(key, workContact);
            });
        }
        return resourceMap;
    }

    @Named("toCertifiedField")
    default CertifiedField<String> toCertifiedField(String field) {
        CertifiedField<String> certifiedField = new CertifiedField<>();
        certifiedField.setCertification(Certification.NONE);
        certifiedField.setValue(field);
        return certifiedField;
    }

    @Named("toCertifiedWorkContact")
    default Map<String, WorkContact> toCertifiedWorkContact(Map<String, WorkContactResponse> workContacts){
        Map<String, WorkContact> resourceMap = new HashMap<>();
        if(workContacts != null && !workContacts.isEmpty()){
            workContacts.forEach((key, value) -> {
                WorkContact workContact = new WorkContact();
                workContact.setEmail(toCertifiedField(value.getEmail()));
                resourceMap.put(key, workContact);
            });
        }
        return resourceMap;
    }

    @Named("toProductInfoMap")
    default Map<String, ProductInfo> toProductInfoMap(List<OnboardedProductResponse> products) {
        Map<String, ProductInfo> productInfoMap = new HashMap<>();
        if (products != null && !products.isEmpty()) {
            Map<String, List<OnboardedProductResponse>> map = products.stream().collect(Collectors.groupingBy(OnboardedProductResponse::getProductId));
            map.forEach((s, onboardedProducts) -> {
                ProductInfo productInfo = new ProductInfo();
                onboardedProducts.forEach(onboardedProduct -> {
                    RoleInfo roleInfo = new RoleInfo();
                    productInfo.setId(onboardedProduct.getProductId());
                    productInfo.setCreatedAt(onboardedProduct.getCreatedAt());
                    roleInfo.setRole(onboardedProduct.getProductRole());
                    roleInfo.setStatus(onboardedProduct.getStatus().name());
                    roleInfo.setSelcRole(it.pagopa.selfcare.commons.base.security.PartyRole.valueOf(onboardedProduct.getRole()).getSelfCareAuthority());
                    roleInfo.setPartyRole(onboardedProduct.getRole());
                    roleInfo.setCreatedAt(onboardedProduct.getCreatedAt());
                    roleInfo.setUpdatedAt(onboardedProduct.getUpdatedAt());
                    if (productInfo.getRoleInfos() == null)
                        productInfo.setRoleInfos(new ArrayList<>());
                    productInfo.getRoleInfos().add(roleInfo);
                });
                productInfoMap.put(s, productInfo);
            });
        }
        return productInfoMap;
    }

    @Mapping(target = "products", expression = "java(toOnboardedProducts(userInstitutionResponse.getProducts()))")
    UserInstitution toUserInstitution(UserInstitutionResponse userInstitutionResponse);

    @Named("toOnboardedProducts")
    List<OnboardedProduct> toOnboardedProducts(List<OnboardedProductResponse> onboardedProductResponse);

    @Mapping(target = "status", expression = "java(it.pagopa.selfcare.dashboard.model.institution.RelationshipState.valueOf(onboardedProductResponse.getStatus().name()))")
    @Mapping(target = "env", expression = "java(it.pagopa.selfcare.commons.base.utils.Env.valueOf(onboardedProductResponse.getEnv().name()))")
    @Mapping(target = "role", expression = "java(it.pagopa.selfcare.commons.base.security.PartyRole.valueOf(onboardedProductResponse.getRole()))")
    OnboardedProduct toOnboardedProducts(OnboardedProductResponse onboardedProductResponse);

    UserOtpEmailInfo toUserOtpEmailInfo(UserOtpEmailInfoResponse userOtpEmailInfoResponse);

    // Methods from the provided class with default implementations
    default ProductInfoResource toUserProductInfoResource(ProductInfo model) {
        ProductInfoResource resource = null;
        if (model != null) {
            resource = new ProductInfoResource();
            resource.setId(model.getId());
            resource.setTitle(model.getTitle());
            resource.setRoleInfos(model.getRoleInfos()
                    .stream()
                    .map(this::toRoleInfoResource)
                    .toList()
            );
        }
        return resource;
    }

    default ProductRoleInfoResource toRoleInfoResource(RoleInfo model) {
        ProductRoleInfoResource resource = null;
        if (model != null) {
            resource = new ProductRoleInfoResource();
            resource.setRelationshipId(model.getRelationshipId());
            resource.setRole(model.getRole());
            resource.setSelcRole(model.getSelcRole());
            resource.setStatus(model.getStatus());
            resource.setPartyRole(model.getPartyRole());
            resource.setCreatedAt(model.getCreatedAt());
            resource.setUpdatedAt(model.getUpdatedAt());
        }
        return resource;
    }

    default InstitutionUserDetailsResource toInstitutionUserDetails(UserInfo model) {
        InstitutionUserDetailsResource resource = toInstitutionUser(model, InstitutionUserDetailsResource::new);
        if (resource != null) {
            Optional.ofNullable(model)
                    .map(UserInfo::getUser)
                    .map(User::getFiscalCode)
                    .ifPresent(resource::setFiscalCode);
        }
        return resource;
    }

    default <T extends InstitutionUserResource> T toInstitutionUser(UserInfo model, Supplier<T> supplier) {
        T resource = null;
        if (model != null) {
            resource = supplier.get();
            resource.setId(UUID.fromString(model.getId()));
            resource.setRole(model.getRole());
            resource.setStatus(model.getStatus());
            if (model.getUser() != null) {
                resource.setName(CertifiedFieldMapper.toValue(model.getUser().getName()));
                resource.setSurname(CertifiedFieldMapper.toValue(model.getUser().getFamilyName()));
                resource.setEmail(CertifiedFieldMapper.toValue(model.getUser().getEmail()));
                resource.setMobilePhone(CertifiedFieldMapper.toValue(model.getUser().getMobilePhone()));
            }
            if (model.getProducts() != null) {
                resource.setProducts(model.getProducts().values().stream()
                        .map(this::toUserProductInfoResource)
                        .toList());
            }
        }
        return resource;
    }

    default ProductUserResource toProductUser(UserInfo model) {
        ProductUserResource resource = null;
        if (model != null) {
            resource = new ProductUserResource();
            resource.setId(UUID.fromString(model.getId()));
            resource.setRole(model.getRole());
            resource.setStatus(model.getStatus());
            if (model.getUser() != null) {
                resource.setName(CertifiedFieldMapper.toValue(model.getUser().getName()));
                resource.setSurname(CertifiedFieldMapper.toValue(model.getUser().getFamilyName()));
                Optional.ofNullable(model.getUser().getWorkContacts())
                        .map(map -> map.get(model.getUserMailUuid() != null ? model.getUserMailUuid() : model.getInstitutionId()))
                        .map(WorkContact::getEmail)
                        .map(CertifiedFieldMapper::toValue)
                        .ifPresent(resource::setEmail);
            }
            if (model.getProducts() != null) {
                resource.setProduct(model.getProducts().values().stream()
                        .map(this::toUserProductInfoResource)
                        .toList()
                        .get(0));
            }
        }
        return resource;
    }

    default List<ProductUserResource> toProductUsers(UserInfo model) {
        List<ProductUserResource> response = new ArrayList<>();
        if (model != null && model.getProducts() != null && !model.getProducts().isEmpty()) {
            model.getProducts().forEach((s, productInfo) -> {
                ProductUserResource resource = new ProductUserResource();
                resource.setId(UUID.fromString(model.getId()));
                resource.setRole(model.getRole());
                resource.setStatus(model.getStatus());
                if (model.getUser() != null) {
                    resource.setName(CertifiedFieldMapper.toValue(model.getUser().getName()));
                    resource.setSurname(CertifiedFieldMapper.toValue(model.getUser().getFamilyName()));
                    resource.setFiscalCode(model.getUser().getFiscalCode());
                    Optional.ofNullable(model.getUser().getWorkContacts())
                            .map(map -> map.get(model.getUserMailUuid()))
                            .map(WorkContact::getEmail)
                            .map(CertifiedFieldMapper::toValue)
                            .ifPresent(resource::setEmail);
                }
                resource.setProduct(toUserProductInfoResource(productInfo));
                resource.setCreatedAt(productInfo.getCreatedAt());
                response.add(resource);
            });
        }
        return response;
    }

    default String mapPartyRole(OnboardedProductResponse product) {
        return product != null ? product.getRole() : null;
    }

    default String mapStatus(OnboardedProductResponse product) {
        return product != null && product.getStatus() != null
                ? product.getStatus().name()
                : null;
    }

}