package it.pagopa.selfcare.dashboard.model.mapper;

import it.pagopa.selfcare.core.generated.openapi.v1.dto.*;
import it.pagopa.selfcare.dashboard.model.institution.*;
import it.pagopa.selfcare.dashboard.model.product.PartyProduct;
import it.pagopa.selfcare.dashboard.model.product.ProductOnBoardingStatus;
import it.pagopa.selfcare.dashboard.model.user.UserInstitution;
import it.pagopa.selfcare.dashboard.model.institution.Billing;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionResponse;
import it.pagopa.selfcare.user.generated.openapi.v1.dto.UserInstitutionRoleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {

    @Mapping(target = "id", source = "institutionId")
    @Mapping(target = "name", source = "institutionName")
    @Mapping(target = "parentDescription", source = "institutionRootName")
    @Mapping(target = "userRole", expression = "java(institutionProducts.getRole())")
    @Mapping(target = "status", expression = "java(institutionProducts.getStatus().name())")
    InstitutionBase toInstitutionBase(UserInstitutionRoleResponse institutionProducts);

    @Mapping(target = "institutionType", source = "institutionResponse.institutionType")
    @Mapping(target = "description" , source = "institutionUpdate.description")
    @Mapping(target = "taxCode" , source = "institutionUpdate.taxCode")
    @Mapping(target = "digitalAddress" , source = "institutionUpdate.digitalAddress")
    @Mapping(target = "address" , source = "institutionUpdate.address")
    @Mapping(target = "zipCode" , source = "institutionUpdate.zipCode")
    @Mapping(target = "city" , source = "institutionResponse.city")
    @Mapping(target = "country" , source = "institutionResponse.country")
    @Mapping(target = "county" , source = "institutionResponse.county")
    @Mapping(target = "paymentServiceProvider" , source = "institutionResponse.paymentServiceProvider")
    @Mapping(target = "dataProtectionOfficer" , source = "institutionResponse.dataProtectionOfficer")
    //@Mapping(target = "billing" , source = "institutionResponse.billing")
    @Mapping(target = "additionalInformations" , source = "institutionUpdate.additionalInformations")
    @Mapping(target = "geographicTaxonomies", ignore = true)
    @Mapping(target = "supportContact", ignore = true)
    InstitutionInfo toInstitutionInfo(InstitutionResponse institutionResponse, InstitutionUpdate institutionUpdate);

    @Mapping(target = "category", expression = "java(getCategory(institution.getAttributes()))")
    Institution toInstitution(InstitutionResponse institution);

    UserInstitution toInstitution(UserInstitutionResponse institutionResponse);

    InstitutionPut toInstitutionPut(UpdateInstitutionResource updateInstitutionResource);

    List<GeographicTaxonomy> toGeographicTaxonomy(List<GeoTaxonomies> geographicTaxonomies);

    Billing toBilling(BillingResponse billing);

    @Mapping(target = "onBoardingStatus", expression = "java(toOnboardedProductState(institutionProduct.getState()))")
    PartyProduct toPartyProduct(InstitutionProduct institutionProduct);

    @Named("toOnboardedProductState")
    default ProductOnBoardingStatus toOnboardedProductState(InstitutionProduct.StateEnum stateEnum) {
        if(stateEnum != null){
            return ProductOnBoardingStatus.valueOf(stateEnum.getValue());
        }
        return null;
    }

    @Named("getCategory")
    default String getCategory(List<AttributesResponse> attributesResponse) {
        if(!CollectionUtils.isEmpty(attributesResponse)) {
            return attributesResponse.get(0).getCode();
        }
        return null;
    }

    @Named("getGeographicTaxonomies")
    default List<GeographicTaxonomy> getGeographicTaxonomies(List<GeoTaxonomies> geographicTaxonomies) {
        if ( geographicTaxonomies == null ) {
            return Collections.emptyList();
        }
        List<GeographicTaxonomy> list = new ArrayList<>();
        for ( GeoTaxonomies geoTaxonomies : geographicTaxonomies ) {
            GeographicTaxonomy geographicTaxonomy = new GeographicTaxonomy();
            geographicTaxonomy.setCode( geoTaxonomies.getCode() );
            geographicTaxonomy.setDesc( geoTaxonomies.getDesc() );
            list.add(geographicTaxonomy);
        }
        return list;
    }

    @Named("getState")
    default RelationshipState getState(String state) {
        if(StringUtils.hasText(state)){
            return RelationshipState.valueOf(state);
        }
        return null;
    }
}
