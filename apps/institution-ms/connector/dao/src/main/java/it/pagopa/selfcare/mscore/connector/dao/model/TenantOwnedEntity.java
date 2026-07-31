package it.pagopa.selfcare.mscore.connector.dao.model;

public interface TenantOwnedEntity {

    String getTenantId();

    void setTenantId(String tenantId);
}
