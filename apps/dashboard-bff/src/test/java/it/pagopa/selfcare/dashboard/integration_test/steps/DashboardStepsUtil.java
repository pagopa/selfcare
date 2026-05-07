package it.pagopa.selfcare.dashboard.integration_test.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.ExtractableResponse;
import it.pagopa.selfcare.commons.base.security.SelfCareAuthority;
import it.pagopa.selfcare.dashboard.integration_test.model.Filter;
import it.pagopa.selfcare.dashboard.integration_test.model.Requests;
import it.pagopa.selfcare.dashboard.integration_test.model.Responses;
import it.pagopa.selfcare.dashboard.model.*;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationRequestDto;
import it.pagopa.selfcare.dashboard.model.delegation.DelegationType;
import it.pagopa.selfcare.dashboard.model.institution.*;
import it.pagopa.selfcare.dashboard.model.product.ProductInfoResource;
import it.pagopa.selfcare.dashboard.model.support.SupportRequestDto;
import it.pagopa.selfcare.dashboard.model.user.UserCountResource;
import it.pagopa.selfcare.dashboard.model.user.UserInstitutionRole;
import it.pagopa.selfcare.dashboard.model.user.UserProductRoles;
import it.pagopa.selfcare.dashboard.model.user_groups.CreateUserGroupDto;
import it.pagopa.selfcare.dashboard.model.user_groups.UpdateUserGroupDto;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Getter
@Setter
public class DashboardStepsUtil {

    protected String errorMessage;
    protected int status;
    protected Filter filter = new Filter();
    protected Responses responses = new Responses();
    protected Requests requests = new Requests();
    protected String token;
    protected ExtractableResponse<?> response;

    public InstitutionUserDetailsResource toInstitutionUserDetailsResource(Map<String, String> entry) {
        InstitutionUserDetailsResource userResource = new InstitutionUserDetailsResource();

        userResource.setId(mapUUID(entry.get("id")));
        userResource.setName(entry.get("name"));
        userResource.setSurname(entry.get("surname"));
        userResource.setEmail(entry.get("email"));
        userResource.setMobilePhone(entry.get("mobilePhone"));
        userResource.setStatus(entry.get("status"));
        userResource.setProducts(mapProductsInfo(entry.get("products")));
        userResource.setRole(mapRole(entry.get("role")));
        userResource.setFiscalCode(entry.get("fiscalCode"));

        return userResource;

    }

    private UUID mapUUID(String uuidString) {
        try {
            return uuidString != null ? UUID.fromString(uuidString) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private SelfCareAuthority mapRole(String role) {
        try {
            return role != null ? SelfCareAuthority.valueOf(role) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<ProductInfoResource> mapProductsInfo(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json != null ? json : "[]",
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ProductInfoResource.class));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Institution toInstitution(Map<String, String> entry) {
        Institution institution = new Institution();
        institution.setId(entry.get("id"));
        institution.setExternalId(entry.get("externalId"));
        institution.setOriginId(entry.get("originId"));
        institution.setDescription(entry.get("description"));
        institution.setDigitalAddress(entry.get("digitalAddress"));
        institution.setAddress(entry.get("address"));
        institution.setZipCode(entry.get("zipCode"));
        institution.setTaxCode(entry.get("taxCode"));
        institution.setOrigin(entry.get("origin"));
        institution.setInstitutionType(entry.get("institutionType"));
        institution.setCategory(entry.get("category"));
        institution.setSubunitCode(entry.get("subunitCode"));
        institution.setSubunitType(entry.get("subunitType"));
        institution.setAooParentCode(entry.get("aooParentCode"));
        institution.setCity(entry.get("city"));
        institution.setCountry(entry.get("country"));
        institution.setCounty(entry.get("county"));
        institution.setDelegation(Boolean.parseBoolean(entry.get("delegation")));

        institution.setAttributes(mapAttributes(entry.get("attributes")));
        institution.setGeographicTaxonomies(mapGeographicTaxonomies(entry.get("geographicTaxonomies")));
        institution.setBilling(mapBilling(entry.get("billing")));
        institution.setPaymentServiceProvider(mapPaymentServiceProvider(entry.get("paymentServiceProvider")));
        institution.setDataProtectionOfficer(mapDataProtectionOfficer(entry.get("dataProtectionOfficer")));
        institution.setSupportContact(mapSupportContact(entry.get("supportContact")));
        institution.setOnboarding(mapOnboardedProducts(entry.get("onboarding")));
        institution.setRootParent(mapRootParent(entry.get("rootParent")));

        return institution;
    }

    private List<String> mapList(String json) {
        return mapList(json, String.class);
    }


    private List<Attribute> mapAttributes(String json) {
        return mapList(json, Attribute.class);
    }

    private List<GeographicTaxonomy> mapGeographicTaxonomies(String json) {
        return mapList(json, GeographicTaxonomy.class);
    }

    private Billing mapBilling(String json) {
        return mapObject(json, Billing.class);
    }

    private PaymentServiceProvider mapPaymentServiceProvider(String json) {
        return mapObject(json, PaymentServiceProvider.class);
    }

    private DataProtectionOfficer mapDataProtectionOfficer(String json) {
        return mapObject(json, DataProtectionOfficer.class);
    }

    private SupportContact mapSupportContact(String json) {
        return mapObject(json, SupportContact.class);
    }

    private List<OnboardedProduct> mapOnboardedProducts(String json) {
        return mapList(json, OnboardedProduct.class);
    }

    private RootParentResponse mapRootParent(String json) {
        return mapObject(json, RootParentResponse.class);
    }

    private <T> List<T> mapList(String json, Class<T> clazz) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json != null ? json : "[]",
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
    private <T> T mapObject(String json, Class<T> clazz) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public InstitutionResource toInstitutionResource(Map<String, String> entry) {
        InstitutionResource institutionResource = new InstitutionResource();
        institutionResource.setId(entry.get("id"));
        institutionResource.setExternalId(entry.get("externalId"));
        institutionResource.setOriginId(entry.get("originId"));
        institutionResource.setOrigin(entry.get("origin"));
        institutionResource.setInstitutionType(entry.get("institutionType"));
        institutionResource.setName(entry.get("name"));
        institutionResource.setCategory(entry.get("category"));
        institutionResource.setCategoryCode(entry.get("categoryCode"));
        institutionResource.setFiscalCode(entry.get("fiscalCode"));
        institutionResource.setMailAddress(entry.get("mailAddress"));
        institutionResource.setUserRole(entry.get("userRole"));
        institutionResource.setStatus(entry.get("status"));
        institutionResource.setAddress(entry.get("address"));
        institutionResource.setZipCode(entry.get("zipCode"));
        institutionResource.setRecipientCode(entry.get("recipientCode"));
        institutionResource.setVatNumberGroup(Boolean.valueOf(entry.get("vatNumberGroup")));
        institutionResource.setVatNumber(entry.get("vatNumber"));
        institutionResource.setSubunitCode(entry.get("subunitCode"));
        institutionResource.setSubunitType(entry.get("subunitType"));
        institutionResource.setAooParentCode(entry.get("aooParentCode"));
        institutionResource.setParentDescription(entry.get("parentDescription"));
        institutionResource.setCity(entry.get("city"));
        institutionResource.setCountry(entry.get("country"));
        institutionResource.setCounty(entry.get("county"));
        institutionResource.setDelegation(Boolean.parseBoolean(entry.get("delegation")));
        try {
            institutionResource.setGeographicTaxonomies(mapGeographicTaxonomiesResource(entry.get("geographicTaxonomies")));
            institutionResource.setSupportContact(mapSupportContactResource(entry.get("supportContact")));
            institutionResource.setProducts(mapProducts(entry.get("products")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }

        return institutionResource;
    }

    private SupportContactResource mapSupportContactResource(String value) throws JsonProcessingException {
        return mapObject(value, SupportContactResource.class);
    }

    private List<GeographicTaxonomyResource> mapGeographicTaxonomiesResource(String value) throws JsonProcessingException {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return new ObjectMapper().readValue(value, new TypeReference<List<GeographicTaxonomyResource>>() {});
    }

    private List<OnboardedProductResource> mapProducts(String value) throws JsonProcessingException {
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        return new ObjectMapper().readValue(value, new TypeReference<List<OnboardedProductResource>>() {});
    }

    public GeographicTaxonomyDto toGeographicTaxonomyDto(Map<String, String> entry) {
        GeographicTaxonomyDto geographicTaxonomyDto = new GeographicTaxonomyDto();
        geographicTaxonomyDto.setCode(entry.get("code"));
        geographicTaxonomyDto.setDesc(entry.get("desc"));
        return geographicTaxonomyDto;
    }

    public UpdateInstitutionDto toUpdateInstitutionDto(Map<String, String> entry) {
        UpdateInstitutionDto updateInstitutionDto = new UpdateInstitutionDto();
        updateInstitutionDto.setDescription(entry.get("description"));
        updateInstitutionDto.setDigitalAddress(entry.get("digitalAddress"));
        return updateInstitutionDto;
    }

    public CreateUserDto toCreateUserDto(Map<String, String> entry) {
        CreateUserDto createUserDto = new CreateUserDto();

        createUserDto.setName(entry.get("name"));
        createUserDto.setSurname(entry.get("surname"));
        createUserDto.setTaxCode(entry.get("taxCode"));
        createUserDto.setEmail(entry.get("email"));
        createUserDto.setRole(entry.get("role"));
        createUserDto.setProductRoles(mapProductRoles(entry.get("productRoles")));
        createUserDto.setToAddOnAggregates(Boolean.valueOf(entry.get("toAddOnAggregates")));

        return createUserDto;
    }

    public UserProductRoles toUserProductRoles(Map<String, String> entry) {
        UserProductRoles createUserDto = new UserProductRoles();

        createUserDto.setRole(entry.get("role"));
        createUserDto.setProductRoles(mapProductRoles(entry.get("productRoles")));
        createUserDto.setToAddOnAggregates(Boolean.valueOf(entry.get("toAddOnAggregates")));

        return createUserDto;
    }

    public UserCountResource toUserCountResource(Map<String, String> entry) {
        UserCountResource userCountResource = new UserCountResource();

        userCountResource.setInstitutionId(entry.get("institutionId"));
        userCountResource.setProductId(entry.get("productId"));
        userCountResource.setRoles(mapList(entry.get("roles")));
        userCountResource.setStatus(mapList(entry.get("status")));
        userCountResource.setCount(Long.valueOf(entry.get("count")));

        return userCountResource;
    }

    private Set<String> mapProductRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return Collections.emptySet();
        }
        return Stream.of(roles.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public DelegationRequestDto toDelegationRequestDto(Map<String, String> entry) {
        DelegationRequestDto delegationRequestDto = new DelegationRequestDto();
        delegationRequestDto.setFrom(entry.get("from"));
        delegationRequestDto.setTo(entry.get("to"));
        delegationRequestDto.setProductId(entry.get("productId"));
        delegationRequestDto.setType(DelegationType.valueOf(entry.get("type")));
        delegationRequestDto.setInstitutionFromName(entry.get("institutionFromName"));
        delegationRequestDto.setInstitutionToName(entry.get("institutionToName"));
        return delegationRequestDto;
    }

    public CreateUserGroupDto toCreateUserGroupDto(Map<String, String> entry) {
        CreateUserGroupDto createUserGroupDtos = new CreateUserGroupDto();
        createUserGroupDtos.setInstitutionId(entry.get("institutionId"));
        createUserGroupDtos.setProductId(entry.get("productId"));
        createUserGroupDtos.setName(entry.get("name"));
        createUserGroupDtos.setDescription(entry.get("description"));
        createUserGroupDtos.setMembers(Optional.ofNullable(entry.get("members")).map(s -> Set.of(entry.get("members").split(",")).stream()
                .map(UUID::fromString).collect(Collectors.toSet())).orElse(null));
        return createUserGroupDtos;
    }

    public UpdateUserGroupDto toUpdateUserGroupDto(Map<String, String> entry) {
        UpdateUserGroupDto updateUserGroupDto = new UpdateUserGroupDto();
        updateUserGroupDto.setName(entry.get("name"));
        updateUserGroupDto.setDescription(entry.get("description"));
        updateUserGroupDto.setMembers(Optional.ofNullable(entry.get("members")).map(s -> Set.of(entry.get("members").split(",")).stream()
                .map(UUID::fromString).collect(Collectors.toSet())).orElse(null));
        return updateUserGroupDto;
    }

    public SupportRequestDto toSupportRequestDto(Map<String, String> entry) {
        SupportRequestDto supportRequestDto = new SupportRequestDto();
        supportRequestDto.setEmail(entry.get("email"));
        supportRequestDto.setUserId(entry.get("userId"));
        supportRequestDto.setInstitutionId(entry.get("institutionId"));
        supportRequestDto.setProductId(entry.get("productId"));
        supportRequestDto.setData(entry.get("data"));
        return supportRequestDto;
    }

    public UserInstitutionRole toUserInstitutionRole(Map<String, String> entry) {
        UserInstitutionRole userInstitutionRole = new UserInstitutionRole();

        userInstitutionRole.setId(entry.get("id"));
        userInstitutionRole.setName(entry.get("name"));
        userInstitutionRole.setSurname(entry.get("surname"));
        userInstitutionRole.setEmail(entry.get("email"));
        userInstitutionRole.setStatus(entry.get("status"));
        userInstitutionRole.setPartyRole(entry.get("partyRole"));
        userInstitutionRole.setFiscalCode(entry.get("fiscalCode"));

        return userInstitutionRole;

    }

}
