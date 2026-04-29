package it.pagopa.selfcare.mscore.model;

import it.pagopa.selfcare.mscore.constant.Origin;
import lombok.Data;

@Data
public class UnitaOrganizzativa {

    private String id;
    private String codiceIpa;
    private String denominazioneEnte;
    private String codiceFiscaleEnte;
    private String codiceUniUo;
    private String codiceUniUoPadre;
    private String codiceUniAoo;
    private String descrizioneUo;
    private String mail1;
    private String tipoMail1;
    private Origin origin;
    private String indirizzo;
    private String CAP;
    private String codiceComuneISTAT;
}
