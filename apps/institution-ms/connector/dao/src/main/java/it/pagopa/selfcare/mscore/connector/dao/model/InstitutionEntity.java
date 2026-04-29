package it.pagopa.selfcare.mscore.connector.dao.model;

import it.pagopa.selfcare.mscore.connector.dao.model.inner.*;
import it.pagopa.selfcare.mscore.constant.Origin;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Document("Institution")
@Sharded(shardKey = {"id"})
@FieldNameConstants(asEnum = true)
public class InstitutionEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private String externalId;
    private Origin origin;
    private String originId;
    private String description;
    private String digitalAddress;
    private String address;
    private String zipCode;
    private String taxCode;
    private String city;
    private String county;
    private String country;
    private String istatCode;
    private BillingEntity billing;
    private List<OnboardingEntity> onboarding;
    private List<GeoTaxonomyEntity> geographicTaxonomies;
    private List<AttributesEntity> attributes;
    private PaymentServiceProviderEntity paymentServiceProvider;
    private DataProtectionOfficerEntity dataProtectionOfficer;
    private String rea;
    private String shareCapital;
    private String businessRegisterPlace;
    private String supportEmail;
    private String supportPhone;
    private boolean imported;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String subunitCode;
    private String subunitType;
    private String parentDescription;
    private String rootParentId;
    private PaAttributesEntity paAttributes;
    private boolean delegation;
    private Boolean isTest;
    private String legalForm;

}
