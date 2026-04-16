package it.pagopa.selfcare.party.registry_proxy.web.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.party.registry_proxy.connector.model.AOO;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AOOResource implements AOO {

    private String id;

    @ApiModelProperty(value = "${swagger.model.aoo.ipaCode}")
    private String codiceIpa;

    @ApiModelProperty(value = "${swagger.model.aoo.parentDescription}")
    private String denominazioneEnte;

    @ApiModelProperty(value = "${swagger.model.aoo.taxCode}")
    private String codiceFiscaleEnte;

    @ApiModelProperty(value = "${swagger.model.aoo.codiceUniAoo}")
    private String codiceUniAoo;

    @ApiModelProperty(value = "${swagger.model.aoo.description}")
    private String denominazioneAoo;

    @ApiModelProperty(value = "${swagger.model.aoo.creationDate}")
    private String dataIstituzione;

    @ApiModelProperty(value = "${swagger.model.aoo.manager.firstname}")
    private String nomeResponsabile;

    @ApiModelProperty(value = "${swagger.model.aoo.manager.lastname}")
    private String cognomeResponsabile;

    @ApiModelProperty(value = "${swagger.model.aoo.manager.email}")
    private String mailResponsabile;

    @ApiModelProperty(value = "${swagger.model.aoo.manager.phone}")
    private String telefonoResponsabile;

    @ApiModelProperty(value = "${swagger.model.aoo.istatCode}")
    private String codiceComuneISTAT;

    @ApiModelProperty(value = "${swagger.model.aoo.geograpichCode}")
    private String codiceCatastaleComune;

    @ApiModelProperty(value = "${swagger.model.aoo.cap}")
    private String CAP;

    @ApiModelProperty(value = "${swagger.model.aoo.legalAddress}")
    private String indirizzo;

    @ApiModelProperty(value = "${swagger.model.aoo.phone}")
    private String telefono;

    @ApiModelProperty(value = "${swagger.model.aoo.fax}")
    private String fax;

    @ApiModelProperty(value = "${swagger.model.aoo.protocol}")
    private String protocolloInformatico;

    @ApiModelProperty(value = "${swagger.model.aoo.uriProtocol}")
    private String URIProtocolloInformatico;

    @ApiModelProperty(value = "${swagger.model.aoo.updateDate}")
    private String dataAggiornamento;

    @ApiModelProperty(value = "${swagger.model.aoo.code}")
    private String codAoo;

    @ApiModelProperty(value = "{swagger.model.*.origin}")
    private Origin origin;

    private String tipoMail1;
    private String mail1;

    private String tipoMail2;
    private String mail2;

    private String tipoMail3;
    private String mail3;
}
