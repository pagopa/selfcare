package it.pagopa.selfcare.onboarding.entity;


import org.bson.codecs.pojo.annotations.BsonProperty;

public class Aggregator {

    @BsonProperty("id")
    private String id;
    private String taxCode;
    private String description;
    private String originId;

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
