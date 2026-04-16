package it.pagopa.selfcare.mscore.web.model.mapper;


import it.pagopa.selfcare.mscore.model.institution.Institution;
import it.pagopa.selfcare.mscore.model.institution.InstitutionGeographicTaxonomies;
import it.pagopa.selfcare.mscore.model.institution.InstitutionUpdate;
import it.pagopa.selfcare.mscore.web.model.institution.*;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import org.mapstruct.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Mapper(componentModel = "spring",  uses = {OnboardingResourceMapper.class}, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface InstitutionResourceMapper {

    @Mapping(target = "aooParentCode", source = "paAttributes.aooParentCode")
    @Mapping(target = "rootParent", source = ".", qualifiedByName = "setRootParent")
    InstitutionResponse toInstitutionResponse(Institution institution);

    @Named("toInstitutionResponseWithType")
    @Mapping(target = "aooParentCode", source = "institution.paAttributes.aooParentCode")
    @Mapping(target = "rootParent", source = "institution", qualifiedByName = "setRootParent")
    @Mapping(target = "institutionType", source = "institution", qualifiedByName = "setInstitutionType")
    @Mapping(target = "origin", source = "institution", qualifiedByName = "setOrigin")
    @Mapping(target = "originId", source = "institution", qualifiedByName = "setOriginId")
    InstitutionResponse toInstitutionResponseWithType(Institution institution, @Context String productId);

    @Named("setRootParent")
    static RootParentResponse setRootParent(Institution institution) {
        if(StringUtils.hasText(institution.getRootParentId())){
            RootParentResponse rootParentResponse = new RootParentResponse();
            rootParentResponse.setId(institution.getRootParentId());
            rootParentResponse.setDescription(institution.getParentDescription());
            return rootParentResponse;
        }
        return null;
    }

    @Named("setInstitutionType")
    default String setInstitutionType(Institution institution, @Context String productId) {
        return Optional.ofNullable(productId)
                .flatMap(pid -> Optional.ofNullable(institution.getOnboarding()))
                .flatMap(onb -> onb.stream().filter(o -> o.getProductId().equals(productId)).findFirst())
                .flatMap(o -> Optional.ofNullable(o.getInstitutionType()))
                .map(InstitutionType::name)
                .orElse(null);
    }

    @Named("setOrigin")
    default String setOrigin(Institution institution, @Context String productId) {
        return Optional.ofNullable(productId)
                .flatMap(pid -> Optional.ofNullable(institution.getOnboarding()))
                .flatMap(onb -> onb.stream().filter(o -> o.getProductId().equals(productId)).findFirst())
                .flatMap(o -> Optional.ofNullable(o.getOrigin()))
                .orElse(institution.getOrigin());
    }

    @Named("setOriginId")
    default String setOriginId(Institution institution, @Context String productId) {
        return Optional.ofNullable(productId)
                .flatMap(pid -> Optional.ofNullable(institution.getOnboarding()))
                .flatMap(onb -> onb.stream().filter(o -> o.getProductId().equals(productId)).findFirst())
                .flatMap(o -> Optional.ofNullable(o.getOriginId()))
                .orElse(institution.getOriginId());
    }

    InstitutionGeographicTaxonomies toInstitutionGeographicTaxonomies(GeoTaxonomies geoTaxonomies);

    @Mapping(target = "geographicTaxonomies", source = ".", qualifiedByName = "toGeographicTaxonomies")
    InstitutionUpdate toInstitutionUpdate(InstitutionPut institutionPut);

    @Named("toGeographicTaxonomies")
    static List<InstitutionGeographicTaxonomies> toGeographicTaxonomies(InstitutionPut institution) {
        return Optional.ofNullable(institution.getGeographicTaxonomyCodes())
                .map(geoTaxonomiesCodes -> geoTaxonomiesCodes.stream()
                        .map(code -> new InstitutionGeographicTaxonomies(code, null))
                        .toList())
                .orElse(null);
    }

    Institution toInstitution(InstitutionRequest request);

    Institution toInstitution(PdaInstitutionRequest request);
}
