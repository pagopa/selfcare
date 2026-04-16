package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.visura;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocalizzazioneWrapper {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "indirizzo_localizzazione")
    private Localizzazione Localizzazione;

    @JacksonXmlProperty(localName = "classificazioni-ateco")
    private ClassificazioniAteco classificazioniAteco;

}