package it.pagopa.selfcare.onboarding.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapi.quarkus.party_registry_proxy_json.api.PdndVisuraInfoCamereControllerApi;

class RegistryProxyServiceImplTest {

  @Test
  void getInstitutionVisuraByTaxCode_shouldDelegateToApi() {
    // given
    PdndVisuraInfoCamereControllerApi pdndApi = Mockito.mock(PdndVisuraInfoCamereControllerApi.class);
    RegistryProxyServiceImpl service = new RegistryProxyServiceImpl(pdndApi);
    byte[] expected = "xml".getBytes();
    when(pdndApi.institutionVisuraDocumentByTaxCodeUsingGET("TAXCODE")).thenReturn(expected);

    // when
    byte[] actual = service.getInstitutionVisuraByTaxCode("TAXCODE");

    // then
    assertArrayEquals(expected, actual);
    verify(pdndApi).institutionVisuraDocumentByTaxCodeUsingGET("TAXCODE");
  }
}
