package it.pagopa.selfcare.onboarding.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import it.pagopa.selfcare.onboarding.utils.CustomOffsetDateTimeDeserializer;

import java.time.OffsetDateTime;

public class OrganizationResponse {
    private String id;
    private String codiceFiscale;
    private String partitaIva;
    private String legalName;
    private String status;
    private String city;
    private String province;
    private String address;
    private String streetNumber;
    private String zipCode;
    private Boolean garante;
    private Boolean garantito;
    private Boolean contraente;
    private String typeOfCounterparty;
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime creationDate;
    @JsonDeserialize(using = CustomOffsetDateTimeDeserializer.class)
    private OffsetDateTime activationDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    public String getPartitaIva() {
        return partitaIva;
    }

    public void setPartitaIva(String partitaIva) {
        this.partitaIva = partitaIva;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStreetNumber() {
        return streetNumber;
    }

    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public Boolean getGarante() {
        return garante;
    }

    public void setGarante(Boolean garante) {
        this.garante = garante;
    }

    public Boolean getGarantito() {
        return garantito;
    }

    public void setGarantito(Boolean garantito) {
        this.garantito = garantito;
    }

    public Boolean getContraente() {
        return contraente;
    }

    public void setContraente(Boolean contraente) {
        this.contraente = contraente;
    }

    public String getTypeOfCounterparty() {
        return typeOfCounterparty;
    }

    public void setTypeOfCounterparty(String typeOfCounterparty) {
        this.typeOfCounterparty = typeOfCounterparty;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public OffsetDateTime getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(OffsetDateTime activationDate) {
        this.activationDate = activationDate;
    }

    @Override
    public String toString() {
        return "OrganizationResponse{" +
                "id='" + id + '\'' +
                ", codiceFiscale='" + codiceFiscale + '\'' +
                ", partitaIva='" + partitaIva + '\'' +
                ", legalName='" + legalName + '\'' +
                ", status='" + status + '\'' +
                ", city='" + city + '\'' +
                ", province='" + province + '\'' +
                ", address='" + address + '\'' +
                ", streetNumber='" + streetNumber + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", garante=" + garante +
                ", garantito=" + garantito +
                ", contraente=" + contraente +
                ", typeOfCounterparty='" + typeOfCounterparty + '\'' +
                ", creationDate=" + creationDate +
                ", activationDate=" + activationDate +
                '}';
    }
}
