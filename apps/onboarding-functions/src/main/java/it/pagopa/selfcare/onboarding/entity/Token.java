package it.pagopa.selfcare.onboarding.entity;

import io.quarkus.mongodb.panache.common.MongoEntity;
import it.pagopa.selfcare.onboarding.common.TokenType;
import org.bson.codecs.pojo.annotations.BsonId;

import java.time.LocalDateTime;


@MongoEntity(collection="tokens")
public class Token {

    @BsonId
    private String id;
    private TokenType type;
    private String onboardingId;
    private String productId;
    private String name;
    private String checksum;
    private String contractVersion;
    private String contractTemplate;
    private String contractSigned;
    private String contractFilename;
    //@Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime activatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TokenType getType() {
        return type;
    }

    public void setType(TokenType type) {
        this.type = type;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getContractVersion() {
        return contractVersion;
    }

    public void setContractVersion(String contractVersion) {
        this.contractVersion = contractVersion;
    }

    public String getContractTemplate() {
        return contractTemplate;
    }

    public void setContractTemplate(String contractTemplate) {
        this.contractTemplate = contractTemplate;
    }

    public String getContractSigned() {
        return contractSigned;
    }

    public void setContractSigned(String contractSigned) {
        this.contractSigned = contractSigned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(LocalDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public String getOnboardingId() {
        return onboardingId;
    }

    public void setOnboardingId(String onboardingId) {
        this.onboardingId = onboardingId;
    }

    public String getContractFilename() {
        return contractFilename;
    }

    public void setContractFilename(String contractFilename) {
        this.contractFilename = contractFilename;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }
}

