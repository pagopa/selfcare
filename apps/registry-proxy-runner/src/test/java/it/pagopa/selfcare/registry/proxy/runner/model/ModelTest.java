package it.pagopa.selfcare.registry.proxy.runner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ModelTest {

  @Test
  void testAnacStationTaxCodePadding() {
    AnacStation station = new AnacStation();
    station.setRawTaxCode("123456789");
    assertEquals("00123456789", station.getTaxCode());

    station.setRawTaxCode("12345678901");
    assertEquals("12345678901", station.getTaxCode());

    station.setRawTaxCode(null);
    assertNull(station.getTaxCode());
  }

  @Test
  void testIvassInsuranceCompanyTaxCodePadding() {
    IvassInsuranceCompany company = new IvassInsuranceCompany();
    company.setRawTaxCode("12345");
    assertEquals("00000012345", company.getTaxCode());

    company.setRawTaxCode("12345678901");
    assertEquals("12345678901", company.getTaxCode());

    company.setRawTaxCode("  12345678901  ");
    assertEquals("12345678901", company.getTaxCode());

    company.setRawTaxCode(null);
    assertNull(company.getTaxCode());
  }

  @Test
  void testIpaInstitutionIndexMapping() {
    IpaInstitution inst = new IpaInstitution();
    inst.setOriginId("origin1");
    inst.setTaxCode("tax1");
    inst.setDescription("desc");
    inst.setCategory("cat");
    inst.setDigitalAddress("pec@pec.it");
    inst.setAddress("address");
    inst.setZipCode("12345");
    inst.setIstatCode("istat");
    inst.setUpdateDate("2023-01-01");

    IpaInstitutionIndex index = IpaInstitutionIndex.fromInstitution(inst);
    assertEquals("tax1", index.getId());
    assertEquals("mergeOrUpload", index.getAction());
    assertEquals("IPA", index.getOrigin());
    assertEquals("tax1", index.getTaxCode());
    assertEquals("origin1", index.getOriginId());
  }
}
