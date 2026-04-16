package it.pagopa.selfcare.mscore.connector.rest.model.registryproxy;

import it.pagopa.selfcare.mscore.constant.Origin;
import lombok.Data;

@Data
public class UoResponse {

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
    private String url;
    private String dataAggiornamento;
}
