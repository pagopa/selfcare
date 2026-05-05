package it.pagopa.selfcare.registry.proxy.runner.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "codiceUniUo")
public class IpaUo {

  @CsvBindByName(column = "Codice_IPA")
  private String codiceIpa;

  @CsvBindByName(column = "Denominazione_ente")
  private String denominazioneEnte;

  @CsvBindByName(column = "Codice_fiscale_ente")
  private String codiceFiscaleEnte;

  @CsvBindByName(column = "Codice_fiscale_sfe")
  private String codiceFiscaleSfe;

  @CsvBindByName(column = "Codice_uni_uo")
  private String codiceUniUo;

  @CsvBindByName(column = "Codice_uni_uo_padre")
  private String codiceUniUoPadre;

  @CsvBindByName(column = "Codice_uni_aoo")
  private String codiceUniAoo;

  @CsvBindByName(column = "Descrizione_uo")
  private String descrizioneUo;

  @CsvBindByName(column = "Mail1")
  private String mail1;

  @CsvBindByName(column = "Mail2")
  private String mail2;

  @CsvBindByName(column = "Mail3")
  private String mail3;

  @CsvBindByName(column = "Data_istituzione")
  private String dataIstituzione;

  @CsvBindByName(column = "Nome_responsabile")
  private String nomeResponsabile;

  @CsvBindByName(column = "Cognome_responsabile")
  private String cognomeResponsabile;

  @CsvBindByName(column = "Mail_responsabile")
  private String mailResponsabile;

  @CsvBindByName(column = "Telefono_responsabile")
  private String telefonoResponsabile;

  @CsvBindByName(column = "Codice_comune_ISTAT")
  private String codiceComuneISTAT;

  @CsvBindByName(column = "Codice_catastale_comune")
  private String codiceCatastaleComune;

  @CsvBindByName(column = "CAP")
  private String cap;

  @CsvBindByName(column = "Indirizzo")
  private String indirizzo;

  @CsvBindByName(column = "Telefono")
  private String telefono;

  @CsvBindByName(column = "Fax")
  private String fax;

  @CsvBindByName(column = "Tipo_Mail1")
  private String tipoMail1;

  @CsvBindByName(column = "Tipo_Mail2")
  private String tipoMail2;

  @CsvBindByName(column = "Tipo_Mail3")
  private String tipoMail3;

  @CsvBindByName(column = "Url")
  private String url;

  @CsvBindByName(column = "Data_aggiornamento")
  private String dataAggiornamento;

  public String getId() {
    return codiceUniUo;
  }
}
