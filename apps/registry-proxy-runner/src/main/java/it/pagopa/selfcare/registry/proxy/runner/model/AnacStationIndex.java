package it.pagopa.selfcare.registry.proxy.runner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnacStationIndex {

  @JsonProperty("@search.action")
  private String action;

  private String id;
  private String originId;
  private String taxCode;
  private String description;
  private String digitalAddress;
  private String anacEngaged;
  private String anacEnabled;
  private String origin;

  public static AnacStationIndex fromStation(AnacStation station) {
    AnacStationIndex index = new AnacStationIndex();
    index.setAction("mergeOrUpload");
    index.setId(station.getId());
    index.setOriginId(station.getOriginId());
    index.setTaxCode(station.getTaxCode());
    index.setDescription(station.getDescription());
    index.setDigitalAddress(station.getDigitalAddress());
    index.setAnacEngaged(station.getAnacEngaged());
    index.setAnacEnabled(station.getAnacEnabled());
    index.setOrigin("ANAC");
    return index;
  }
}
