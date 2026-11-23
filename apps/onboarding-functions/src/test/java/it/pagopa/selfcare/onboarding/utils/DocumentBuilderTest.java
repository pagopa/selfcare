package it.pagopa.selfcare.onboarding.utils;

import it.pagopa.selfcare.onboarding.common.ProductId;
import it.pagopa.selfcare.onboarding.entity.Billing;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.RtpServiceType;
import it.pagopa.selfcare.onboarding.mapper.AttachmentPdfRequestMapper;
import it.pagopa.selfcare.onboarding.mapper.ContractPdfRequestMapperImpl;
import it.pagopa.selfcare.onboarding.mapper.DocumentBuilderRequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapi.quarkus.document_json.model.ContractPdfRequest;
import org.openapi.quarkus.product_json.model.ProductResponse;
import org.openapi.quarkus.user_registry_json.model.UserResource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class DocumentBuilderTest {

  private final DocumentBuilder documentBuilder = new DocumentBuilder(
      new ContractPdfRequestMapperImpl(),
      mock(AttachmentPdfRequestMapper.class),
      mock(DocumentBuilderRequestMapper.class));

  private Onboarding onboarding;
  private ProductResponse product;
  private UserResource manager;
  private UserResource delegate;

  @BeforeEach
  void setUp() {
    Institution institution = new Institution();
    institution.setId("institution-id");
    Billing billing = new Billing();
    billing.setVatNumber("12345678901");
    onboarding = new Onboarding();
    onboarding.setId("onboarding-id");
    onboarding.setInstitution(institution);
    onboarding.setBilling(billing);
    onboarding.setPricingPlan("pricing-plan");
    onboarding.setIsAggregator(true);
    product = new ProductResponse();
    product.setProductId(ProductId.PROD_RTP.getValue());
    product.setTitle("Product title");
    manager = new UserResource();
    manager.setFiscalCode("manager-tax-code");
    delegate = new UserResource();
    delegate.setFiscalCode("delegate-tax-code");
  }

  @ParameterizedTest
  @MethodSource("rtpServiceSelections")
  void toContractPdfRequest_mapsRtpServicesAndCommonFields(List<RtpServiceType> services) {
    onboarding.setRtpServiceTypes(services);

    ContractPdfRequest request = toRequest();

    assertCommonFields(request);
    if (services == null) {
      assertNull(request.getRtpServiceTypes());
    } else {
      assertEquals(
          services.stream().map(Enum::name).toList(),
          request.getRtpServiceTypes().stream().map(Enum::name).toList());
    }
  }

  private ContractPdfRequest toRequest() {
    return documentBuilder.toContractPdfRequest(
        onboarding, manager, List.of(delegate), product, "contract-template", "aggregates-url");
  }

  private void assertCommonFields(ContractPdfRequest request) {
    assertEquals("onboarding-id", request.getOnboardingId());
    assertEquals(product.getProductId(), request.getProductId());
    assertEquals("Product title", request.getProductName());
    assertEquals("pricing-plan", request.getPricingPlan());
    assertEquals(Boolean.TRUE, request.getIsAggregator());
    assertEquals("institution-id", request.getInstitution().getId());
    assertEquals("12345678901", request.getBilling().getVatNumber());
    assertEquals("manager-tax-code", request.getManager().getTaxCode());
    assertEquals(1, request.getDelegates().size());
    assertEquals("delegate-tax-code", request.getDelegates().get(0).getTaxCode());
    assertEquals("contract-template", request.getContractTemplatePath());
    assertEquals("aggregates-url", request.getAggregatesCsvBaseUrl());
  }

  private static Stream<Arguments> rtpServiceSelections() {
    return Stream.of(
        Arguments.of(List.of(RtpServiceType.RTP)),
        Arguments.of(List.of(RtpServiceType.REPOSITORY_CONSULTATION)),
        Arguments.of(List.of(RtpServiceType.RTP, RtpServiceType.REPOSITORY_CONSULTATION)),
        Arguments.of(List.of()),
        Arguments.of((Object) null));
  }
}
