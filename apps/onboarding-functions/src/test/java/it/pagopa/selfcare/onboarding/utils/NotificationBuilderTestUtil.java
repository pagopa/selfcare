package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.common.OnboardingStatus;
import it.pagopa.selfcare.onboarding.common.Origin;
import it.pagopa.selfcare.onboarding.common.WorkflowType;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.PaymentServiceProvider;
import it.pagopa.selfcare.onboarding.entity.Token;
import org.openapi.quarkus.core_json.model.DataProtectionOfficerResponse;
import org.openapi.quarkus.core_json.model.InstitutionResponse;
import org.openapi.quarkus.core_json.model.PaymentServiceProviderResponse;
import org.openapi.quarkus.core_json.model.RootParentResponse;
import org.openapi.quarkus.party_registry_proxy_json.api.GeographicTaxonomiesApi;
import org.openapi.quarkus.party_registry_proxy_json.api.InstitutionApi;
import org.openapi.quarkus.party_registry_proxy_json.model.GeographicTaxonomyResource;
import org.openapi.quarkus.party_registry_proxy_json.model.InstitutionResource;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class NotificationBuilderTestUtil {

  public static final String INSTITUTION_ID = "i1";
  public static final String TOKEN_ID = "t1";
  public static final String PRODUCT_ID = "prod";

  public static Token createToken() {
    Token token = new Token();
    token.setId(TOKEN_ID);
    token.setProductId(PRODUCT_ID);
    token.setContractSigned("contractSigned");
    return token;
  }

  public static InstitutionResponse createInstitution() {
    InstitutionResponse institutionResponse = new InstitutionResponse();
    institutionResponse.setAddress("address");
    institutionResponse.setAooParentCode("aooParentCode");
    institutionResponse.setAttributes(new ArrayList<>());
    institutionResponse.setBusinessRegisterPlace("businessRegisterPlace");
    institutionResponse.setCity("city");
    institutionResponse.setCountry("country");
    institutionResponse.setCounty("county");
    institutionResponse.setCreatedAt(OffsetDateTime.parse("2020-11-01T10:00:00Z"));
    institutionResponse.setDataProtectionOfficer(new DataProtectionOfficerResponse());
    institutionResponse.setDelegation(true);
    institutionResponse.setDescription("description");
    institutionResponse.setDigitalAddress("digitalAddress");
    institutionResponse.setExternalId("externalId");
    institutionResponse.setGeographicTaxonomies(new ArrayList<>());
    institutionResponse.setId(INSTITUTION_ID);
    institutionResponse.setImported(true);
    institutionResponse.setInstitutionType("PA");
    institutionResponse.setIstatCode("istatCode");
    institutionResponse.setOnboarding(new ArrayList<>());
    institutionResponse.setOrigin("IPA");
    institutionResponse.setOriginId("originId");
    institutionResponse.setPaymentServiceProvider(mockPaymentServiceProvider());
    institutionResponse.setRea("rea");
    institutionResponse.setRootParent(new RootParentResponse());
    institutionResponse.setShareCapital("shareCapital");
    institutionResponse.setSubunitCode("subunitCode");
    institutionResponse.setSubunitType("subunitType");
    institutionResponse.setSupportEmail("supportEmail");
    institutionResponse.setSupportPhone("supportPhone");
    institutionResponse.setTaxCode("taxCode");
    institutionResponse.setUpdatedAt(OffsetDateTime.parse("2020-11-02T10:05:00Z"));
    institutionResponse.setZipCode("zipCode");

    return institutionResponse;
  }

  public static PaymentServiceProviderResponse mockPaymentServiceProvider() {
    PaymentServiceProviderResponse paymentServiceProvider = new PaymentServiceProviderResponse();
    paymentServiceProvider.setAbiCode("abiCode");
    paymentServiceProvider.setBusinessRegisterNumber("businessRegisterNumber");
    paymentServiceProvider.setLegalRegisterName("legalRegisterName");
    paymentServiceProvider.setLegalRegisterNumber("legalRegisterNumber");
    paymentServiceProvider.setVatNumberGroup(true);
    return paymentServiceProvider;
  }

  public static Onboarding createOnboarding(
      OnboardingStatus status,
      OffsetDateTime createdAt,
      OffsetDateTime activatedAt,
      OffsetDateTime updateAt,
      OffsetDateTime deletedAt) {
    Onboarding onboarding = new Onboarding();
    onboarding.setProductId(PRODUCT_ID);
    onboarding.setId(TOKEN_ID);
    onboarding.setStatus(status);
    onboarding.setCreatedAt(Objects.nonNull(createdAt) ? createdAt.toLocalDateTime() : null);
    onboarding.setActivatedAt(Objects.nonNull(activatedAt) ? activatedAt.toLocalDateTime() : null);
    onboarding.setUpdatedAt(Objects.nonNull(updateAt) ? updateAt.toLocalDateTime() : null);
    onboarding.setDeletedAt(Objects.nonNull(deletedAt) ? deletedAt.toLocalDateTime() : null);
    onboarding.setWorkflowType(WorkflowType.IMPORT);

    Institution institution = new Institution();
    institution.setId(INSTITUTION_ID);
    institution.setInstitutionType(InstitutionType.PSP);
    institution.setOrigin(Origin.IPA);
    institution.setOriginId("originId");
    institution.setTaxCode("taxCode");
    PaymentServiceProvider paymentServiceProvider = new PaymentServiceProvider();
    paymentServiceProvider.setContractType("contractType");
    paymentServiceProvider.setProviderNames(List.of("providerName"));
    paymentServiceProvider.setContractId("contractId");
    onboarding.setInstitution(institution);
    onboarding.getInstitution().setPaymentServiceProvider(paymentServiceProvider);
    return onboarding;
  }

  public static void mockPartyRegistryProxy(
      InstitutionApi registryProxyInstitutionsApi,
      GeographicTaxonomiesApi geographicTaxonomiesApi,
      InstitutionResponse institution) {
    InstitutionResource institutionProxyInfoMock = mockInstitutionResource();
    institutionProxyInfoMock.setTaxCode(institution.getExternalId());

    GeographicTaxonomyResource geographicTaxonomiesMock = mockGeographicTaxonomies();
    geographicTaxonomiesMock.setIstatCode(institutionProxyInfoMock.getIstatCode());

    when(registryProxyInstitutionsApi.findInstitutionUsingGET(any(), any(), any()))
        .thenReturn(institutionProxyInfoMock);
    when(geographicTaxonomiesApi.retrieveGeoTaxonomiesByCodeUsingGET(any()))
        .thenReturn(geographicTaxonomiesMock);
  }

  public static GeographicTaxonomyResource mockGeographicTaxonomies() {
    GeographicTaxonomyResource geographicTaxonomiesMock = new GeographicTaxonomyResource();
    geographicTaxonomiesMock.setIstatCode("istatCode");
    geographicTaxonomiesMock.setCode("code");
    geographicTaxonomiesMock.setProvinceId("province");
    geographicTaxonomiesMock.setProvinceAbbreviation("provinceAbbreviation");
    geographicTaxonomiesMock.setRegionId("region");
    geographicTaxonomiesMock.setCountry("country");
    geographicTaxonomiesMock.setCountryAbbreviation("countryAbbreviation");
    geographicTaxonomiesMock.setDesc("desc");
    return geographicTaxonomiesMock;
  }

  public static InstitutionResource mockInstitutionResource() {
    InstitutionResource institutionProxyInfoMock = new InstitutionResource();
    institutionProxyInfoMock.setIstatCode("istatCode");
    institutionProxyInfoMock.setAddress("address");
    institutionProxyInfoMock.setAoo("aoo");
    institutionProxyInfoMock.setCategory("category");
    institutionProxyInfoMock.setId("id");
    institutionProxyInfoMock.setDescription("description");
    return institutionProxyInfoMock;
  }
}
