package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.controller.response.TokenResponse;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Token;
import it.pagopa.selfcare.onboarding.util.InstitutionUtils;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "cdi")
public interface TokenMapper {

    TokenResponse toResponse(Token entity);

    @Mapping(target = "id", source = "onboarding.id")
    @Mapping(target = "onboardingId", source = "onboarding.id")
    @Mapping(target = "contractTemplate", expression = "java(getContractTemplatePath(onboarding, product))")
    @Mapping(target = "contractVersion", expression = "java(getContractTemplateVersion(onboarding, product))")
    @Mapping(target = "contractSigned", source = "contractImported.filePath")
    @Mapping(target = "contractFilename", source = "contractImported.fileName")
    @Mapping(target = "createdAt", source = "contractImported.createdAt")
    @Mapping(target = "updatedAt", source = "contractImported.createdAt")
    @Mapping(target = "productId", source = "onboarding.productId")
    @Mapping(target = "type", constant = "INSTITUTION")
    @Mapping(target = "activatedAt", ignore = true)
    Token toModel(Onboarding onboarding, Product product, OnboardingImportContract contractImported);

    @Mapping(target = "id", source = "onboarding.id")
    @Mapping(target = "onboardingId", source = "onboarding.id")
    @Mapping(target = "contractTemplate", source = "contractTemplate.contractTemplatePath")
    @Mapping(target = "contractVersion", source = "contractTemplate.contractTemplateVersion")
    @Mapping(target = "type", constant = "INSTITUTION")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "activatedAt", ignore = true)
    Token toModel(Onboarding onboarding, Product product, ContractTemplate contractTemplate);

    default String getContractTemplatePath(Onboarding onboarding, Product product) {
        ContractTemplate contractTemplate = product.getInstitutionContractTemplate(
                InstitutionUtils.getCurrentInstitutionType(onboarding));
        return contractTemplate.getContractTemplatePath();
    }

    default String getContractTemplateVersion(Onboarding onboarding, Product product) {
        ContractTemplate contractTemplate = product.getInstitutionContractTemplate(
                InstitutionUtils.getCurrentInstitutionType(onboarding));
        return contractTemplate.getContractTemplateVersion();
    }
}
