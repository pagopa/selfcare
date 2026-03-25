package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.visura;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InfoAttivita {

 @JacksonXmlProperty(localName = "attivita-prevalente")
 private String attivitaPrevalente;

 @JacksonXmlProperty(localName = "classificazioni-ateco")
 private ClassificazioniAteco classificazioniAteco;

 @JacksonXmlProperty(isAttribute = true, localName = "c-stato")
 private String disabledStateInstitution;

 @JacksonXmlProperty(isAttribute = true, localName = "stato")
 private String descriptionStateInstitution;

}
