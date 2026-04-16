package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.visura;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassificazioneAteco {

    @JacksonXmlProperty(isAttribute = true, localName = "c-attivita")
    private String codiceAttivita;

    @JacksonXmlProperty(isAttribute = true, localName = "attivita")
    private String descrizioneAttivita;

    @JacksonXmlProperty(isAttribute = true, localName = "c-importanza")
    private String codiceImportanza;

}
