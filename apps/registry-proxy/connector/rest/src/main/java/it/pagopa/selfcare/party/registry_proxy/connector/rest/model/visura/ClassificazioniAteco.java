package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.visura;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassificazioniAteco {

 @JacksonXmlElementWrapper(useWrapping = false)
 @JacksonXmlProperty(localName = "classificazione-ateco")
 private List<ClassificazioneAteco> classificazioniAteco;

}
