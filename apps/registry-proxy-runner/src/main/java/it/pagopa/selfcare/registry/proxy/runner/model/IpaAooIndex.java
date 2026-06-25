package it.pagopa.selfcare.registry.proxy.runner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IpaAooIndex {

  @JsonProperty("@search.action")
  private String action;

  private String id;
  private String codiceIpa;
  private String denominazioneEnte;
  private String codiceFiscaleEnte;
  private String codiceUniAoo;
  private String denominazioneAoo;
  private String mail1;
  private String mail2;
  private String mail3;
  private String codAoo;
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
  private String protocolloInformatico;
  private String uriProtocolloInformatico;
  private String updateDate;

  public static IpaAooIndex fromAoo(IpaAoo aoo) {
    IpaAooIndex index = new IpaAooIndex();
    index.setAction("mergeOrUpload");
    index.setId(aoo.getId());
    index.setCodiceIpa(aoo.getCodiceIpa());
    index.setDenominazioneEnte(aoo.getDenominazioneEnte());
    index.setCodiceFiscaleEnte(aoo.getCodiceFiscaleEnte());
    index.setCodiceUniAoo(aoo.getCodiceUniAoo());
    index.setDenominazioneAoo(aoo.getDenominazioneAoo());
    index.setMail1(aoo.getMail1());
    index.setMail2(aoo.getMail2());
    index.setMail3(aoo.getMail3());
    index.setCodAoo(aoo.getCodAoo());
    index.setDataIstituzione(aoo.getDataIstituzione());
    index.setNomeResponsabile(aoo.getNomeResponsabile());
    index.setCognomeResponsabile(aoo.getCognomeResponsabile());
    index.setMailResponsabile(aoo.getMailResponsabile());
    index.setTelefonoResponsabile(aoo.getTelefonoResponsabile());
    index.setCodiceComuneISTAT(aoo.getCodiceComuneISTAT());
    index.setCodiceCatastaleComune(aoo.getCodiceCatastaleComune());
    index.setCap(aoo.getCap());
    index.setIndirizzo(aoo.getIndirizzo());
    index.setTelefono(aoo.getTelefono());
    index.setFax(aoo.getFax());
    index.setTipoMail1(aoo.getTipoMail1());
    index.setTipoMail2(aoo.getTipoMail2());
    index.setTipoMail3(aoo.getTipoMail3());
    index.setProtocolloInformatico(aoo.getProtocolloInformatico());
    index.setUriProtocolloInformatico(aoo.getUriProtocolloInformatico());
    index.setUpdateDate(aoo.getDataAggiornamento());
    return index;
  }
}
