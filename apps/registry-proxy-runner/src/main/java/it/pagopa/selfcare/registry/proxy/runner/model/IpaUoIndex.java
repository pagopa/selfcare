package it.pagopa.selfcare.registry.proxy.runner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpaUoIndex {

  @JsonProperty("@search.action")
  private String action;

  private String id;
  private String codiceIpa;
  private String denominazioneEnte;
  private String codiceFiscaleEnte;
  private String codiceFiscaleSfe;
  private String codiceUniUo;
  private String codiceUniUoPadre;
  private String codiceUniAoo;
  private String descrizioneUo;
  private String mail1;
  private String mail2;
  private String mail3;
  private String dataIstituzione;
  private String nomeResponsabile;
  private String cognomeResponsabile;
  private String mailResponsabile;
  private String telefonoResponsabile;
  private String codiceComuneISTAT;
  private String codiceCatastaleComune;
  private String cap;
  private String indirizzo;
  private String telefono;
  private String fax;
  private String tipoMail1;
  private String tipoMail2;
  private String tipoMail3;
  private String url;
  private String updateDate;

  public static IpaUoIndex fromUo(IpaUo uo) {
    IpaUoIndex index = new IpaUoIndex();
    index.setAction("mergeOrUpload");
    index.setId(uo.getId());
    index.setCodiceIpa(uo.getCodiceIpa());
    index.setDenominazioneEnte(uo.getDenominazioneEnte());
    index.setCodiceFiscaleEnte(uo.getCodiceFiscaleEnte());
    index.setCodiceFiscaleSfe(uo.getCodiceFiscaleSfe());
    index.setCodiceUniUo(uo.getCodiceUniUo());
    index.setCodiceUniUoPadre(uo.getCodiceUniUoPadre());
    index.setCodiceUniAoo(uo.getCodiceUniAoo());
    index.setDescrizioneUo(uo.getDescrizioneUo());
    index.setMail1(uo.getMail1());
    index.setMail2(uo.getMail2());
    index.setMail3(uo.getMail3());
    index.setDataIstituzione(uo.getDataIstituzione());
    index.setNomeResponsabile(uo.getNomeResponsabile());
    index.setCognomeResponsabile(uo.getCognomeResponsabile());
    index.setMailResponsabile(uo.getMailResponsabile());
    index.setTelefonoResponsabile(uo.getTelefonoResponsabile());
    index.setCodiceComuneISTAT(uo.getCodiceComuneISTAT());
    index.setCodiceCatastaleComune(uo.getCodiceCatastaleComune());
    index.setCap(uo.getCap());
    index.setIndirizzo(uo.getIndirizzo());
    index.setTelefono(uo.getTelefono());
    index.setFax(uo.getFax());
    index.setTipoMail1(uo.getTipoMail1());
    index.setTipoMail2(uo.getTipoMail2());
    index.setTipoMail3(uo.getTipoMail3());
    index.setUrl(uo.getUrl());
    index.setUpdateDate(uo.getDataAggiornamento());
    return index;
  }
}
