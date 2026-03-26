package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.Payment;
import it.pagopa.selfcare.product.entity.Product;
import java.util.List;
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
  @Mapping(target = "manager", source = "manager")
  @Mapping(target = "delegates", source = "delegates")
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

  @Mapping(target = "id", expression = "java(userResource.getId().toString())")
  @Mapping(target = "name", source = "name.value")
  @Mapping(target = "surname", source = "familyName.value")
  @Mapping(target = "taxCode", source = "fiscalCode")
  @Mapping(target = "email", source = "email.value")
  UserPdfData toUserPdfData(UserResource userResource);

  List<UserPdfData> toUserPdfData(List<UserResource> userResources);
}
