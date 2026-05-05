package it.pagopa.selfcare.registry.proxy.runner.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "taxCode")
public class IpaInstitution {

  @CsvBindByName(column = "Codice_IPA")
  private String originId;

  @CsvBindByName(column = "Codice_fiscale_ente")
  private String taxCode;

  @CsvBindByName(column = "Codice_Categoria")
  private String category;

  @CsvBindByName(column = "Denominazione_ente")
  private String description;

  @CsvBindByName(column = "Mail1")
  private String digitalAddress;

  @CsvBindByName(column = "Indirizzo")
  private String address;

  @CsvBindByName(column = "CAP")
  private String zipCode;

  @CsvBindByName(column = "Codice_comune_ISTAT")
  private String istatCode;

  @CsvBindByName(column = "Data_aggiornamento")
  private String updateDate;

  public String getId() {
    return taxCode;
  }
}
