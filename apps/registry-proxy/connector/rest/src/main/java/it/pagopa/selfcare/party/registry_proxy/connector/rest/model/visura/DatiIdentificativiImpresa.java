package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.visura;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatiIdentificativiImpresa {

    @JacksonXmlProperty(isAttribute = true, localName = "c-fiscale")
    private String businessTaxId;

    @JacksonXmlProperty(isAttribute = true, localName = "cciaa")
    private String cciaa;

    @JacksonXmlProperty(isAttribute = true, localName = "n-rea")
    private String nRea;

    @JacksonXmlProperty(isAttribute = true, localName = "denominazione")
    private String businessName;

    @JacksonXmlProperty(localName = "indirizzo-posta-certificata")
    private String digitalAddress;

    @JacksonXmlProperty(localName = "indirizzo-localizzazione")
    private Localizzazione localizzazione;

    @JacksonXmlProperty(localName = "forma-giuridica")
    private String legalForm;

    @JacksonXmlProperty(isAttribute = true, localName = "partita-iva")
    private String vatNumber;

    @JacksonXmlProperty(isAttribute = true, localName = "stato-impresa")
    private String statusCompanyRI;

    @JacksonXmlProperty(isAttribute = true, localName = "stato-ditta")
    private String statusCompanyRD;

}
