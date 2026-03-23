package it.pagopa.selfcare.party.registry_proxy.connector.rest.model;

import com.opencsv.bean.CsvBindByName;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Institution;
import it.pagopa.selfcare.party.registry_proxy.connector.model.Origin;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "id")
public abstract class IPAOpenDataTemplate implements Institution {

    private String id;
    @CsvBindByName(column = "Codice_IPA")
    private String originId;
    private String o;
    private String ou;
    private String aoo;
    @CsvBindByName(column = "Codice_fiscale_ente")
    private String taxCode;
    @CsvBindByName(column = "Codice_Categoria")
    private String category;
    @CsvBindByName(column = "Denominazione_ente")
    private String description;
    @CsvBindByName(column = "Mail1")
    private String digitalAddress;
    @CsvBindByName(column = "Indirizzo")
    private String address;
    @CsvBindByName(column = "CAP")
    private String zipCode;
    @CsvBindByName(column = "Codice_comune_ISTAT")
    private String istatCode;


    @Override
    public String getId() {
        return taxCode;
    }

    @Override
    public Origin getOrigin() {
        return Origin.IPA;
    }

}
