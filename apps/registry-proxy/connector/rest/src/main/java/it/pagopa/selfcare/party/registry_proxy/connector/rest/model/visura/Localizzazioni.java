package it.pagopa.selfcare.party.registry_proxy.connector.rest.model.visura;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Localizzazioni {

 @JacksonXmlElementWrapper(useWrapping = false)
 @JacksonXmlProperty(localName = "localizzazione")
 private List<LocalizzazioneWrapper> localizzazioni;

}
