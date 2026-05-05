package it.pagopa.selfcare.registry.proxy.runner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IvassInsuranceCompanyIndex {

  @JsonProperty("@search.action")
  private String action;

  private String id;
  private String originId;
  private String taxCode;
  private String description;
  private String digitalAddress;
  private String workType;
  private String registerType;
  private String address;
  private String origin;

  public static IvassInsuranceCompanyIndex fromInsuranceCompany(IvassInsuranceCompany company) {
    IvassInsuranceCompanyIndex index = new IvassInsuranceCompanyIndex();
    index.setAction("mergeOrUpload");
    index.setId(company.getId());
    index.setOriginId(company.getOriginId());
    index.setTaxCode(company.getTaxCode());
    index.setDescription(company.getDescription());
    index.setDigitalAddress(company.getDigitalAddress());
    index.setWorkType(company.getWorkType());
    index.setRegisterType(company.getRegisterType());
    index.setAddress(company.getAddress());
    index.setOrigin("IVASS");
    return index;
  }
}
