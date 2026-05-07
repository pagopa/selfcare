package it.pagopa.selfcare.registry.proxy.runner.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "code")
public class IpaCategory {

  @CsvBindByName(column = "Codice_categoria")
  private String code;

  @CsvBindByName(column = "Nome_categoria")
  private String name;

  @CsvBindByName(column = "Tipologia_categoria")
  private String kind;

  public String getId() {
    return "IPA_" + code;
  }
}
