package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.commons.base.utils.ProductId;
import it.pagopa.selfcare.external_api.model.onboarding.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OnboardingMapperCustom {

  public static OnboardingImportContract fromDto(ImportContractDto model) {
    OnboardingImportContract resource = null;
    if (model != null) {
      resource = new OnboardingImportContract();
      resource.setFileName(model.getFileName());
      resource.setFilePath(model.getFilePath());
      resource.setContractType(model.getContractType());
      resource.setCreatedAt(model.getOnboardingDate());
      if (model.getOnboardingDate() != null) {
        resource.setActivatedAt(model.getOnboardingDate());
      } else {
        resource.setActivatedAt(OffsetDateTime.now());
      }
    }
    return resource;
  }

  public static Billing fromDto(BillingDataDto model) {
    Billing resource = null;
    if (model != null) {
      resource = new Billing();
      resource.setVatNumber(model.getVatNumber());
      resource.setRecipientCode(model.getRecipientCode());
      if (model.getPublicServices() != null) {
        resource.setPublicServices(model.getPublicServices());
      }
      if (model.getTaxCodeInvoicing() != null) {
        resource.setTaxCodeInvoicing(model.getTaxCodeInvoicing());
      }
    }
    return resource;
  }

  public static OnboardingImportData toOnboardingImportData(
      String externalId, OnboardingImportDto model) {
    OnboardingImportData resource = null;
    if (model != null) {
      resource = new OnboardingImportData();
      resource.setInstitutionExternalId(externalId);
      resource.setProductId(ProductId.PROD_IO.getValue());
      resource.setUsers(
          model.getUsers().stream().map(UserMapperCustom::toUser).toList());
      resource.setContractImported(fromDto(model.getImportContract()));
      resource.setBilling(new Billing());
      resource.setInstitutionUpdate(new InstitutionUpdate());
      resource.getInstitutionUpdate().setImported(true);
    }
    return resource;
  }

  public static OnboardingData toOnboardingData(String externalId, OnboardingImportDto model) {
    OnboardingData resource = null;
    if (model != null) {
      resource = new OnboardingData();
      resource.setInstitutionExternalId(externalId);
      resource.setProductId(ProductId.PROD_IO.getValue());
      resource.setUsers(Objects.nonNull(model.getUsers()) ?
          model.getUsers().stream().map(UserMapperCustom::toUser).toList() : List.of());
      resource.setContractImported(fromDto(model.getImportContract()));
      resource.setBilling(new Billing());
      resource.setInstitutionUpdate(new InstitutionUpdate());
      resource.getInstitutionUpdate().setTaxCode(externalId);
      resource.getInstitutionUpdate().setImported(true);
      resource.setSendCompleteOnboardingEmail(Boolean.TRUE);
    }
    return resource;
  }

  public static OnboardingData toOnboardingData(
      String externalId, String productId, OnboardingDto model) {
    OnboardingData resource = null;
    if (model != null) {
      resource = new OnboardingData();
      resource.setUsers(
          model.getUsers().stream().map(UserMapperCustom::toUser).toList());
      resource.setInstitutionExternalId(externalId);
      resource.setProductId(productId);
      resource.setOrigin(model.getOrigin());
      resource.setPricingPlan(model.getPricingPlan());
      resource.setInstitutionUpdate(mapInstitutionUpdate(model));
      if (model.getBillingData() != null) {
        resource.setBilling(fromDto(model.getBillingData()));
      }
      resource.setInstitutionType(model.getInstitutionType());
    }
    return resource;
  }

  private static InstitutionUpdate mapInstitutionUpdate(OnboardingDto dto) {
    InstitutionUpdate resource = null;
    if (dto != null && dto.getBillingData() != null) {
      resource = new InstitutionUpdate();
      resource.setAddress(dto.getBillingData().getRegisteredOffice());
      resource.setDigitalAddress(dto.getBillingData().getDigitalAddress());
      resource.setZipCode(dto.getBillingData().getZipCode());
      resource.setDescription(dto.getBillingData().getBusinessName());
      resource.setTaxCode(dto.getBillingData().getTaxCode());
      resource.setPaymentServiceProvider(mapPaymentServiceProvider(dto.getPspData()));
      resource.setDataProtectionOfficer(mapDataProtectionOfficer(dto.getPspData()));
      resource.setGeographicTaxonomies(
          dto.getGeographicTaxonomies().stream()
              .map(GeographicTaxonomyMapper::fromDto)
              .toList());
      if (dto.getCompanyInformations() != null) {
        resource.setRea(dto.getCompanyInformations().getRea());
        resource.setShareCapital(dto.getCompanyInformations().getShareCapital());
        resource.setBusinessRegisterPlace(dto.getCompanyInformations().getBusinessRegisterPlace());
      }
      if (dto.getAssistanceContacts() != null) {
        resource.setSupportEmail(dto.getAssistanceContacts().getSupportEmail());
        resource.setSupportPhone(dto.getAssistanceContacts().getSupportPhone());
      }
      resource.setImported(false);
    }
    return resource;
  }

  private static PaymentServiceProvider mapPaymentServiceProvider(PspDataDto dto) {
    PaymentServiceProvider resource = null;
    if (dto != null) {
      resource = new PaymentServiceProvider();
      resource.setAbiCode(dto.getAbiCode());
      resource.setBusinessRegisterNumber(dto.getBusinessRegisterNumber());
      resource.setLegalRegisterName(dto.getLegalRegisterName());
      resource.setLegalRegisterNumber(dto.getLegalRegisterNumber());
      resource.setContractId(dto.getContractId());
      resource.setContractType(dto.getContractType());
      resource.setProviderNames(dto.getProviderNames());
      resource.setVatNumberGroup(dto.getVatNumberGroup());
    }
    return resource;
  }

  private static DataProtectionOfficer mapDataProtectionOfficer(PspDataDto dto) {
    DataProtectionOfficer resource = null;
    if (dto != null && dto.getDpoData() != null) {
      resource = new DataProtectionOfficer();
      resource.setAddress(dto.getDpoData().getAddress());
      resource.setEmail(dto.getDpoData().getEmail());
      resource.setPec(dto.getDpoData().getPec());
    }
    return resource;
  }
}
