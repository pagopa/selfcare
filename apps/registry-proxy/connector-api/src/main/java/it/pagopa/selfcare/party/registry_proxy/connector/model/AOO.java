package it.pagopa.selfcare.party.registry_proxy.connector.model;

public interface AOO {

    default String getId() {
        return getCodiceUniAoo();
    }
    String getCodiceIpa();
    String getDenominazioneEnte();
    String getCodiceFiscaleEnte();
    String getCodiceUniAoo();
    String getDenominazioneAoo();
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
    String getMail1();
    String getTipoMail1();
    String getMail2();
    String getTipoMail2();
    String getMail3();
    String getTipoMail3();
    String getProtocolloInformatico();
    String getURIProtocolloInformatico();
    String getDataAggiornamento();
    String getCodAoo();
    Origin getOrigin();


    enum Field implements SearchField {
        ID("id"),
        CODICE_IPA("codiceIpa"),
        DENOMINAZIONE_ENTE("denominazioneEnte"),
        CODICE_FISCALE_ENTE("codiceFiscaleEnte"),
        CODICE_UNI_AOO("codiceUniAoo"),
        DENOMINAZIONE_AOO("denominazioneAoo"),
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
        MAIL1("mail1"),
        MAIL2("mail2"),
        MAIL3("mail3"),
        TIPO_MAIL1("tipoMail1"),
        TIPO_MAIL2("tipoMail2"),
        TIPO_MAIL3("tipoMail3"),
        PROTOCOLLO_INFORMATICO("protocolloInformatico"),
        URI_PROTOCOLLO_INFORMATICO("URIProtocolloInformatico"),
        DATA_AGGIORNAMENTO("dataAggiornamento"),
        COD_AOO("codAoo"),
        ORIGIN("origin");

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
