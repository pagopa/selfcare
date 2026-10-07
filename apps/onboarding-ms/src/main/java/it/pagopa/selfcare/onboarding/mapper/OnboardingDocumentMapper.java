package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.service.util.ProductConfigUtils;
import it.pagopa.selfcare.onboarding.util.InstitutionUtils;
import java.time.LocalDateTime;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.document_json.model.OnboardingDocumentRequest;
import org.openapi.quarkus.product_json.model.ContractTemplateConfig;
import org.openapi.quarkus.product_json.model.ProductResponse;

@Mapper(componentModel = "cdi")
public interface OnboardingDocumentMapper {

    @Mapping(target = "onboardingId", source = "onboarding.id")
    @Mapping(target = "templatePath", expression = "java(getContractTemplatePath(onboarding, product))")
    @Mapping(target = "templateVersion", expression = "java(getContractTemplateVersion(onboarding, product))")
    @Mapping(target = "contractFilePath", source = "contractImported.filePath")
    @Mapping(target = "contractFileName", source = "contractImported.fileName")
    @Mapping(target = "contractCreatedAt", expression = "java(toLocalDateTime(contractImported))")
    @Mapping(target = "productId", source = "onboarding.productId")
    OnboardingDocumentRequest toRequest(
            Onboarding onboarding,
            ProductResponse product,
            OnboardingImportContract contractImported
    );

    default String getContractTemplatePath(Onboarding onboarding, ProductResponse product) {
        ContractTemplateConfig contractTemplate = ProductConfigUtils.institutionContractTemplate(product,
                InstitutionUtils.getCurrentInstitutionType(onboarding));
        return contractTemplate.getPath();
    }

    default String getContractTemplateVersion(Onboarding onboarding, ProductResponse product) {
        ContractTemplateConfig contractTemplate = ProductConfigUtils.institutionContractTemplate(product,
                InstitutionUtils.getCurrentInstitutionType(onboarding));
        return contractTemplate.getVersion();
    }

    default LocalDateTime toLocalDateTime(OnboardingImportContract contractImported) {
        if (Objects.nonNull(contractImported) && Objects.nonNull(contractImported.getCreatedAt())) {
            return contractImported.getCreatedAt();
        }
        return null;
    }
}
