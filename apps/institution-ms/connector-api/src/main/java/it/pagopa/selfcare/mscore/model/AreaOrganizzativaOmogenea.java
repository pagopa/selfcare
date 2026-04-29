package it.pagopa.selfcare.mscore.model;

import it.pagopa.selfcare.mscore.constant.Origin;
import lombok.Data;

@Data
public class AreaOrganizzativaOmogenea {

    private String id;
    private String codiceIpa;
    private String denominazioneEnte;
    private String codiceFiscaleEnte;
    private String codiceUniAoo;
    private String denominazioneAoo;
    private String mail1;
    private String tipoMail1;
    private String codAoo;
    private Origin origin;
    private String indirizzo;
    private String CAP;
    private String codiceComuneISTAT;
}
