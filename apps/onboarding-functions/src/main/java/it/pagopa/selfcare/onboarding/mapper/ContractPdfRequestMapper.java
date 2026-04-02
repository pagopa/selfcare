package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.*;
import it.pagopa.selfcare.product.entity.Product;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapi.quarkus.document_json.model.BillingPdfData;
import org.openapi.quarkus.document_json.model.ContractPdfRequest;
import org.openapi.quarkus.document_json.model.InstitutionPdfData;
import org.openapi.quarkus.document_json.model.PaymentPdfData;
import org.openapi.quarkus.document_json.model.UserPdfData;
import org.openapi.quarkus.user_registry_json.model.UserResource;

@Mapper(componentModel = "cdi")
public interface ContractPdfRequestMapper {

  @Mapping(target = "onboardingId", source = "onboarding.id")
  @Mapping(target = "productId", source = "product.id")
  @Mapping(target = "productName", source = "product.title")
  @Mapping(target = "pricingPlan", source = "onboarding.pricingPlan")
  @Mapping(target = "isAggregator", source = "onboarding.isAggregator")
  @Mapping(target = "institution", source = "onboarding.institution")
  @Mapping(target = "manager", expression = "java(toUserPdfData(manager, onboarding))")
  @Mapping(target = "delegates", expression = "java(toUserPdfDataList(delegates, onboarding))")
  @Mapping(target = "billing", source = "onboarding.billing")
  @Mapping(target = "payment", source = "onboarding.payment")
  @Mapping(target = "contractTemplatePath", source = "contractTemplatePath")
  @Mapping(target = "aggregatesCsvBaseUrl", source = "aggregatesCsvBaseUrl")
  ContractPdfRequest toRequest(
          Onboarding onboarding,
          UserResource manager,
          List<UserResource> delegates,
          Product product,
          String contractTemplatePath,
          String aggregatesCsvBaseUrl);

  InstitutionPdfData toInstitutionPdfData(Institution institution);

  BillingPdfData toBillingPdfData(Billing billing);

  PaymentPdfData toPaymentPdfData(Payment payment);

  @Mapping(target = "id", expression = "java(userResource.getId() != null ? userResource.getId().toString() : null)")
  @Mapping(target = "name", source = "userResource.name.value")
  @Mapping(target = "surname", source = "userResource.familyName.value")
  @Mapping(target = "taxCode", source = "userResource.fiscalCode")
  @Mapping(target = "userMailUuid", expression = "java(extractUserMailUuid(userResource, onboarding))")
  @Mapping(target = "email", expression = "java(extractWorkContactEmail(userResource, onboarding))")
  UserPdfData toUserPdfData(UserResource userResource, Onboarding onboarding);

  default List<UserPdfData> toUserPdfDataList(List<UserResource> userResources, Onboarding onboarding) {
    if (userResources == null) {
      return null;
    }
    return userResources.stream()
            .map(user -> toUserPdfData(user, onboarding))
            .collect(Collectors.toList());
  }

  default String extractUserMailUuid(UserResource userResource, Onboarding onboarding) {
    if (userResource == null || userResource.getId() == null || onboarding == null || onboarding.getUsers() == null) {
      return null;
    }

    String userId = userResource.getId().toString();

    return onboarding.getUsers().stream()
            .filter(u -> u.getId().equals(userId))
            .map(User::getUserMailUuid)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
  }

  default String extractWorkContactEmail(UserResource userResource, Onboarding onboarding) {
    String mailUuid = extractUserMailUuid(userResource, onboarding);

    if (mailUuid != null && userResource.getWorkContacts() != null) {
      var workContact = userResource.getWorkContacts().get(mailUuid);
      if (workContact != null && workContact.getEmail() != null) {
        return workContact.getEmail().getValue();
      }
    }
    return null;
  }
}