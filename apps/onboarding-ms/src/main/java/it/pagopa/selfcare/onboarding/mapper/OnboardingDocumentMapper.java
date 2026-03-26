package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.controller.request.OnboardingImportContract;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.util.InstitutionUtils;
import it.pagopa.selfcare.product.entity.ContractTemplate;
import it.pagopa.selfcare.product.entity.Product;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.document_json.model.OnboardingDocumentRequest;

@Mapper(componentModel = "cdi")
public interface OnboardingDocumentMapper {

    @Mapping(target = "onboardingId", source = "onboarding.id")
    @Mapping(target = "templatePath", expression = "java(getContractTemplatePath(onboarding, product))")
    @Mapping(target = "templateVersion", expression = "java(getContractTemplateVersion(onboarding, product))")
    @Mapping(target = "contractFilePath", source = "contractImported.filePath")
    @Mapping(target = "contractFileName", source = "contractImported.fileName")
    @Mapping(target = "contractCreatedAt", expression = "java(toOffsetDateTime(contractImported.getCreatedAt()))")
    @Mapping(target = "productId", source = "onboarding.productId")
    OnboardingDocumentRequest toRequest(
            Onboarding onboarding,
            Product product,
            OnboardingImportContract contractImported
    );

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

    default OffsetDateTime toOffsetDateTime(java.time.LocalDateTime localDateTime) {
        if (Objects.isNull(localDateTime)) {
            return null;
        }

        return localDateTime.atOffset(java.time.ZoneOffset.UTC);
    }
}
