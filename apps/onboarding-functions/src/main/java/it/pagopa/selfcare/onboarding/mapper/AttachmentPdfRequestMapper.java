package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.product.entity.AttachmentTemplate;
import it.pagopa.selfcare.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.document_json.model.AttachmentPdfRequest;
import org.openapi.quarkus.document_json.model.InstitutionPdfData;
import org.openapi.quarkus.document_json.model.UserPdfData;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@Mapper(componentModel = "cdi")
public interface AttachmentPdfRequestMapper {
  @Mapping(target = "onboardingId", source = "onboarding.id")
  @Mapping(target = "productId", source = "product.id")
  @Mapping(target = "productName", source = "product.title")
  @Mapping(target = "attachmentTemplatePath", source = "attachment.templatePath")
  @Mapping(target = "attachmentName", source = "attachment.name")
  @Mapping(target = "institution", source = "onboarding.institution")
  @Mapping(target = "manager", source = "manager")
  AttachmentPdfRequest toRequest(
      Onboarding onboarding, AttachmentTemplate attachment, Product product, UserResource manager);

  InstitutionPdfData toInstitutionPdfData(Institution institution);

  @Mapping(target = "id", expression = "java(userResource.getId().toString())")
  @Mapping(target = "name", source = "name.value")
  @Mapping(target = "surname", source = "familyName.value")
  @Mapping(target = "taxCode", source = "fiscalCode")
  @Mapping(target = "email", source = "email.value")
  UserPdfData toUserPdfData(UserResource userResource);
}
