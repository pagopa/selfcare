package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

@Data
public class DummyAOO implements AOO {

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
    private Origin origin;

    private String dataIstituzione;
    private String nomeResponsabile;
    private String cognomeResponsabile;
    private String mailResponsabile;
    private String telefonoResponsabile;
    private String codiceComuneISTAT;
    private String codiceCatastaleComune;
    private String CAP;
    private String indirizzo;
    private String telefono;
    private String fax;
    private String tipoMail1;
    private String tipoMail2;
    private String tipoMail3;
    private String protocolloInformatico;
    private String URIProtocolloInformatico;
    private String dataAggiornamento;
}