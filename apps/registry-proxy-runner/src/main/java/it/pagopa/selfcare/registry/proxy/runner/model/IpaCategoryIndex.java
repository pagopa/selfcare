package it.pagopa.selfcare.registry.proxy.runner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpaCategoryIndex {

  @JsonProperty("@search.action")
  private String action;

  private String id;
  private String code;
  private String name;
  private String kind;
  private String origin;

  public static IpaCategoryIndex fromCategory(IpaCategory category) {
    IpaCategoryIndex index = new IpaCategoryIndex();
    index.setAction("mergeOrUpload");
    index.setId(category.getId());
    index.setCode(category.getCode());
    index.setName(category.getName());
    index.setKind(category.getKind());
    index.setOrigin("IPA");
    return index;
  }
}
