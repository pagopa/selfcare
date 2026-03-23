package it.pagopa.selfcare.party.registry_proxy.connector.model;

public interface UO {

    default String getId() {
        return getCodiceUniUo();
    }

    String getCodiceIpa();

    String getDenominazioneEnte();
    String getCodiceFiscaleEnte();
    String getCodiceFiscaleSfe();
    String getCodiceUniUo();
    String getCodiceUniUoPadre();
    String getCodiceUniAoo();
    String getDescrizioneUo();


    Origin getOrigin();

    String getMail1();
    String getMail2();
    String getMail3();

    String getTipoMail1();
    String getTipoMail2();
    String getTipoMail3();

    String getDataIstituzione();
    String getNomeResponsabile();
    String getCognomeResponsabile();
    String getMailResponsabile();
    String getTelefonoResponsabile();
    String getCodiceComuneISTAT();
    String getCodiceCatastaleComune();
    String getCAP();
    String getIndirizzo();
    String getTelefono();
    String getFax();
    String getUrl();
    String getDataAggiornamento();

    enum Field implements SearchField {
        ID("id"),
        CODICE_IPA("codiceIpa"),
        DENOMINAZIONE_ENTE("denominazioneEnte"),
        CODICE_FISCALE_ENTE("codiceFiscaleEnte"),
        CODICE_FISCALE_SFE("codiceFiscaleSfe"),
        CODICE_UNI_UO("codiceUniUo"),
        CODICE_UNI_UO_PADRE("codiceUniUoPadre"),
        CODICE_UNI_AOO("codiceUniAoo"),
        DESCRIZIONE_UO("descrizioneUo"),
        MAIL1("mail1"),
        MAIL2("mail2"),
        MAIL3("mail3"),
        ORIGIN("origin"),

        DATA_ISTITUTIONE("dataIstituzione"),
        NOME_RESPONSABILE("nomeResponsabile"),
        COGNOME_RESPONSABILE("cognomeResponsabile"),
        MAIL_RESPONSABILE("mailResponsabile"),
        TELEFONO_RESPONSABILE("telefonoResponsabile"),
        CODICE_COMUNE_ISTAT("codiceComuneISTAT"),
        CODICE_CATASTALE_COMUNE("codiceCatastaleComune"),
        CAP("CAP"),
        INDIRIZZO("indirizzo"),
        TELEFONO("telefono"),
        FAX("fax"),
        TIPO_MAIL1("tipoMail1"),
        TIPO_MAIL2("tipoMail2"),
        TIPO_MAIL3("tipoMail3"),
        URL("url"),
        DATA_AGGIORNAMENTO("dataAggiornamento");

        private final String name;

        Field(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

}
