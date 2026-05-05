package it.pagopa.selfcare.registry.proxy.runner.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "taxCode")
public class AnacStation {

  @CsvBindByName(column = "codiceIPA")
  private String originId;

  @CsvBindByName(column = "codiceFiscaleGestore")
  private String rawTaxCode;

  @CsvBindByName(column = "denominazioneGestore")
  private String description;

  @CsvBindByName(column = "PEC")
  private String digitalAddress;

  @CsvBindByName(column = "ANAC_incaricato")
  private String anacEngaged;

  @CsvBindByName(column = "ANAC_abilitato")
  private String anacEnabled;

  public String getTaxCode() {
    if (rawTaxCode != null && rawTaxCode.length() < 11) {
      return StringUtils.leftPad(rawTaxCode, 11, "0");
    }
    return rawTaxCode;
  }

  public String getId() {
    return getTaxCode();
  }
}
