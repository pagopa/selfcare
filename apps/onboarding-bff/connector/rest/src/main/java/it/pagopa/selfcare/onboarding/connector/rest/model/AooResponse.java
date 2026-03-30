package it.pagopa.selfcare.onboarding.connector.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.selfcare.commons.base.utils.Origin;
import lombok.Data;

@Data
public class AooResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("codiceIpa")
    private String ipaCode;

    @JsonProperty("denominazioneEnte")
    private String institutionDescription;

    @JsonProperty("codiceFiscaleEnte")
    private String institutionFiscalCode;

    @JsonProperty("codiceUniAoo")
    private String uniAooCode;

    @JsonProperty("denominazioneAoo")
    private String aooDescription;

    @JsonProperty("mail1")
    private String mail1;

    @JsonProperty("codAoo")
    private String codAoo;

    @JsonProperty("origin")
    private Origin origin;

    @JsonProperty("dataIstituzione")
    private String institutionDate;

    @JsonProperty("nomeResponsabile")
    private String delegateName;
    @JsonProperty("cognomeResponsabile")
    private String delegateSurname;
    @JsonProperty("mailResponsabile")
    private String delegateMail;
    @JsonProperty("telefonoResponsabile")
    private String delegatePhoneNumber;
    @JsonProperty("codiceComuneISTAT")
    private String municipalIstatCode;
    @JsonProperty("codiceCatastaleComune")
    private String municipalCadastralCode;
    @JsonProperty("CAP")
    private String cap;
    @JsonProperty("indirizzo")
    private String address;
    @JsonProperty("telefono")
    private String phoneNumber;
    @JsonProperty("fax")
    private String fax;
    @JsonProperty("tipoMail1")
    private String mailType;
    @JsonProperty("protocolloInformatico")
    private String itProtocol;
    @JsonProperty("URIProtocolloInformatico")
    private String uriProtocol;
    @JsonProperty("dataAggiornamento")
    private String updateDate;

}
