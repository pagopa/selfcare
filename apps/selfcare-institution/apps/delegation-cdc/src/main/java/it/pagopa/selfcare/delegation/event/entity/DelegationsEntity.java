package it.pagopa.selfcare.delegation.event.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntity;
import it.pagopa.selfcare.delegation.event.constant.DelegationState;
import it.pagopa.selfcare.delegation.event.constant.DelegationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.bson.codecs.pojo.annotations.BsonProperty;


@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection = "Delegations")
@FieldNameConstants(asEnum = true)
public class DelegationsEntity extends ReactivePanacheMongoEntity {

    @BsonProperty("_id")
    private String id;
    private String from;
    private String institutionFromName;
    private String institutionToName;
    private String institutionFromRootName;
    private String to;
    private String toTaxCode;
    private String fromTaxCode;
    private String toType;
    private String fromType;
    private String productId;
    private DelegationType type;
    private DelegationState status;
    private String createdAt;
    private String updatedAt;
    private String closedAt;
    private Boolean isTest;

}
