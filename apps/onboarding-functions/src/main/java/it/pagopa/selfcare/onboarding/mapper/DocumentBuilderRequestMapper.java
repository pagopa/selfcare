package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.OnboardingWorkflow;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.document_json.model.DocumentBuilderRequest;
import org.openapi.quarkus.document_json.model.DocumentType;

@Mapper(componentModel = "cdi")
public interface DocumentBuilderRequestMapper {

    @Mapping(target = "onboardingId", source = "onboarding.id")
    @Mapping(target = "productId", source = "onboarding.productId")
    @Mapping(
        target = "documentType",
        expression = "java(org.openapi.quarkus.document_json.model.DocumentType.fromValue(onboardingWorkflow.getDocumentType().name()))")
    @Mapping(target = "attachmentName", expression = "java(onboardingWorkflow.getPdfFormatFilename())")
    @Mapping(target = "templatePath", expression = "java(onboardingWorkflow.getContractTemplatePath(product))")
    @Mapping(target = "templateVersion", expression = "java(onboardingWorkflow.getContractTemplateVersion(product))")
    @Mapping(target = "productTitle", source = "product.title")
    DocumentBuilderRequest toRequest(Onboarding onboarding, Product product, OnboardingWorkflow onboardingWorkflow);

    @Mapping(target = "onboardingId", source = "onboarding.id")
    @Mapping(target = "productId", source = "onboarding.productId")
    @Mapping(target = "documentType", expression = "java(documentType)")
    @Mapping(target = "attachmentName", source = "attachmentTemplate.name")
    @Mapping(target = "templatePath", source = "attachmentTemplate.templatePath")
    @Mapping(target = "templateVersion", source = "attachmentTemplate.templateVersion")
    @Mapping(target = "productTitle", source = "product.title")
    DocumentBuilderRequest toRequest(Onboarding onboarding, Product product, AttachmentTemplate attachmentTemplate, DocumentType documentType);
}
