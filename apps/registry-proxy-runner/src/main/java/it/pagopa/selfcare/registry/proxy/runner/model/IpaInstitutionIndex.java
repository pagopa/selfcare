package it.pagopa.selfcare.registry.proxy.runner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpaInstitutionIndex {

  @JsonProperty("@search.action")
  private String action;

  @JsonProperty("@search.score")
  private Double score;

  private String id;
  private String originId;
  private String taxCode;
  private String description;
  private String category;
  private String digitalAddress;
  private String address;
  private String zipCode;
  private String istatCode;
  private String origin;
  private String updateDate;

  public static IpaInstitutionIndex fromInstitution(IpaInstitution institution) {
    IpaInstitutionIndex index = new IpaInstitutionIndex();
    index.setAction("mergeOrUpload");
    index.setId(institution.getTaxCode());
    index.setOriginId(institution.getOriginId());
    index.setTaxCode(institution.getTaxCode());
    index.setDescription(institution.getDescription());
    index.setCategory(institution.getCategory());
    index.setDigitalAddress(institution.getDigitalAddress());
    index.setAddress(institution.getAddress());
    index.setZipCode(institution.getZipCode());
    index.setIstatCode(institution.getIstatCode());
    index.setOrigin("IPA");
    index.setUpdateDate(institution.getUpdateDate());
    return index;
  }
}
