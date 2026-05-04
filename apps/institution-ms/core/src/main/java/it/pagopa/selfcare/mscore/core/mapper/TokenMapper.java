package it.pagopa.selfcare.mscore.core.mapper;

import it.pagopa.selfcare.mscore.model.institution.Onboarding;
import it.pagopa.selfcare.mscore.model.onboarding.Token;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TokenMapper {

    @Mapping(target = "id" , source = "onboarding.tokenId")
    @Mapping(target = "institutionId" , source = "institutionId")
    @Mapping(target = "productId" , source = "productId")
    @Mapping(target = "createdAt" , source = "onboarding.createdAt")
    @Mapping(target = "updatedAt" , source = "onboarding.createdAt")
    @Mapping(target = "status" , source = "onboarding.status")
    @Mapping(target = "contractSigned" , source = "onboarding.contract")
    Token toToken(Onboarding onboarding, String institutionId, String productId);

}
