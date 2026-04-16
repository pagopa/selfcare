package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.institution;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstitutionResponse {

  @NotBlank
  private String id;
  private String externalId;
  @NotBlank
  private String origin;
  @NotBlank
  private String originId;
  private String description;
  private String institutionType;
  private String digitalAddress;
  private String address;
  private String zipCode;
  private String taxCode;
  private String city;
  private String county;
  private String country;
  private String istatCode;
  private List<GeoTaxonomies> geographicTaxonomies;
  private List<AttributesResponse> attributes;
  private List<OnboardedProductResponse> onboarding;
  private PaymentServiceProviderResponse paymentServiceProvider;
  private DataProtectionOfficerResponse dataProtectionOfficer;
  private RootParentResponse rootParent;
  private String rea;
  private String shareCapital;
  private String businessRegisterPlace;
  private String supportEmail;
  private String supportPhone;
  private boolean imported;
  private String subunitCode;
  private String subunitType;
  private String aooParentCode;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
  private boolean delegation;
  private String logo;
  private Boolean isTest;
  private String legalForm;

}
