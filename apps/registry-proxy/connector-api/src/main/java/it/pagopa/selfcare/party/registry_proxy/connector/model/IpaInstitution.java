package it.pagopa.selfcare.party.registry_proxy.connector.model;

import lombok.Data;

@Data
public class IpaInstitution implements Institution {

    private String id;
    private String originId;
    private String taxCode;
    private String description;
    private String category;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String istatCode;
    private Origin origin;
    private String updateDate;

    @Override
    public String getO() {
        return null;
    }

    @Override
    public String getOu() {
        return null;
    }

    @Override
    public String getAoo() {
        return null;
    }

}
