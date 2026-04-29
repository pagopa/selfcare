package it.pagopa.selfcare.delegation.event.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoEntityBase;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@MongoEntity(collection = "Institution")
@FieldNameConstants(asEnum = true)
public class Institution extends ReactivePanacheMongoEntityBase {

    private String id;
    private String externalId;
    private String originId;
    private String description;
    private InstitutionType institutionType;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String taxCode;
    private String city;
    private String county;
    private String country;
    private String istatCode;
    private List<OnboardingEntity> onboarding;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private boolean imported;
    private String subunitCode;
    private String subunitType;
    private String parentDescription;
    private String rootParentId;
    private boolean delegation;

}
