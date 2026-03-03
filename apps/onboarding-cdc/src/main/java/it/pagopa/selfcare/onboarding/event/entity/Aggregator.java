package it.pagopa.selfcare.onboarding.event.entity;

import lombok.Data;
import org.bson.codecs.pojo.annotations.BsonProperty;

@Data
public class Aggregator {

    @BsonProperty("id")
    private String id;
    private String taxCode;
    private String description;
    private String originId;

}
