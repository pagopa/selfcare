package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.visura;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Localizzazione {

 @JacksonXmlProperty(isAttribute = true, localName = "comune")
 private String comune;

 @JacksonXmlProperty(isAttribute = true, localName = "provincia")
 private String provincia;

 @JacksonXmlProperty(isAttribute = true, localName = "cap")
 private String cap;

 @JacksonXmlProperty(isAttribute = true, localName = "toponimo")
 private String toponimo;

 @JacksonXmlProperty(isAttribute = true, localName = "via")
 private String via;

 @JacksonXmlProperty(isAttribute = true, localName = "n-civico")
 private String civico;

}
