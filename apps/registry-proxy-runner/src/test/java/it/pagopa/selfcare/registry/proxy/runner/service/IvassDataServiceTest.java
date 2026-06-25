package it.pagopa.selfcare.registry.proxy.runner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.pagopa.selfcare.registry.proxy.runner.client.IvassRestClient;
import it.pagopa.selfcare.registry.proxy.runner.model.IvassInsuranceCompany;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IvassDataServiceTest {

  @Mock IvassRestClient ivassRestClient;
  @Mock AzureBlobStorageService storageService;

  @InjectMocks IvassDataService ivassDataService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
        ivassDataService, "registryTypesAdmitted", Arrays.asList("ElencoI", "SezioneI"));
    ReflectionTestUtils.setField(
        ivassDataService, "workTypesAdmitted", Arrays.asList("VITA", "MISTA"));
  }

  @Test
  void fetch() throws Exception {
    String csvContent =
        "CODICE_IVASS;CODICE_FISCALE;DENOMINAZIONE_IMPRESA;PEC;TIPO_LAVORO;TIPO_ALBO;INDIRIZZO_SEDE_LEGALE_RAPPRESENTANZA_IN_ITALIA\n"
            + "IV1;12345;Company 1;pec1@test.it;VITA;Elenco I - test;Address 1\n"
            + "IV2;23456;Company 2;pec2@test.it;DANNI;Elenco I - test;Address 2\n"
            + // Filtered by work type
            "IV3;34567;Company 3;pec3@test.it;MISTA;Sezione II - test;Address 3\n"
            + // Filtered by register type
            "IV4;45678;Company 4;;VITA;Elenco I - test;Address 4\n"; // Filtered by empty PEC

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      ZipEntry entry = new ZipEntry("test.csv");
      zos.putNextEntry(entry);
      zos.write(csvContent.getBytes());
      zos.closeEntry();
    }

    byte[] zipBytes = baos.toByteArray();

    when(ivassRestClient.retrieveDataSource()).thenReturn(zipBytes);

    List<IvassInsuranceCompany> companies = ivassDataService.fetch();

    assertEquals(1, companies.size());
    assertEquals("IV1", companies.get(0).getOriginId());
    assertEquals("00000012345", companies.get(0).getTaxCode());
  }

  @Test
  void fetch_error() {
    when(ivassRestClient.retrieveDataSource()).thenThrow(new RuntimeException("Error"));
    List<IvassInsuranceCompany> companies = ivassDataService.fetch();
    assertTrue(companies.isEmpty());
  }

  @Test
  void testRemoveUtf8Bom() {
    byte[] csv = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b', 'c'};
    byte[] result = ivassDataService.removeUtf8Bom(csv);
    assertEquals(3, result.length);
    assertEquals('a', result[0]);
  }
}
