package it.pagopa.selfcare.party.registry_proxy.web.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import it.pagopa.selfcare.party.registry_proxy.connector.model.UO;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UOResource implements UO {

    private String id;

    @ApiModelProperty(value = "${swagger.model.uo.ipaCode}")
    private String codiceIpa;

    @ApiModelProperty(value= "${swagger.model.uo.parentDescription}")
    private String denominazioneEnte;

    @ApiModelProperty(value= "${swagger.model.uo.taxCode}")
    private String codiceFiscaleEnte;

    @ApiModelProperty(value= "${swagger.model.uo.taxCodeInvoicing}")
    private String codiceFiscaleSfe;

    @ApiModelProperty(value = "${swagger.model.uo.codiceUniUo}")
    private String codiceUniUo;

    @ApiModelProperty(value = "${swagger.model.uo.parentCode}")
    private String codiceUniUoPadre;

    @ApiModelProperty(value = "${swagger.model.aoo.codiceUniAoo}")
    private String codiceUniAoo;

    @ApiModelProperty(value = "${swagger.model.uo.description}")
    private String descrizioneUo;

    private String mail1;
    private String mail2;
    private String mail3;

    @ApiModelProperty(value = "${swagger.model.*.origin}")
    private Origin origin;

    @ApiModelProperty(value = "${swagger.model.uo.creationDate}")
    private String dataIstituzione;

    @ApiModelProperty(value = "${swagger.model.uo.manager.firstname}")
    private String nomeResponsabile;

    @ApiModelProperty(value = "${swagger.model.uo.manager.lastname}")
    private String cognomeResponsabile;

    @ApiModelProperty(value = "${swagger.model.uo.manager.email}")
    private String mailResponsabile;

    @ApiModelProperty(value = "${swagger.model.uo.manager.phone}")
    private String telefonoResponsabile;

    @ApiModelProperty(value = "${swagger.model.uo.istatCode}")
    private String codiceComuneISTAT;

    @ApiModelProperty(value = "${swagger.model.uo.geograpichCode}")
    private String codiceCatastaleComune;

    @ApiModelProperty(value = "${swagger.model.uo.cap}")
    private String CAP;

    @ApiModelProperty(value = "${swagger.model.uo.legalAddress}")
    private String indirizzo;

    @ApiModelProperty(value = "${swagger.model.uo.phone}")
    private String telefono;

    @ApiModelProperty(value = "${swagger.model.uo.fax}")
    private String fax;

    private String tipoMail1;
    private String tipoMail2;
    private String tipoMail3;

    @ApiModelProperty(value = "${swagger.model.uo.url}")
    private String url;

    @ApiModelProperty(value = "${swagger.model.uo.updateDate}")
    private String dataAggiornamento;
}
