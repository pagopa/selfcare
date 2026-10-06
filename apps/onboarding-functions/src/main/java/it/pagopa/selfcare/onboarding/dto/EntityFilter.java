package it.pagopa.selfcare.onboarding.dto;

public class EntityFilter {
  private String value;
  private String tenantId;

  public EntityFilter() {
  }

  private EntityFilter(Builder builder) {
    this.value = builder.value;
    this.tenantId = builder.tenantId;
  }

  public static EntityFilter.Builder builder() {
    return new EntityFilter.Builder();
  }

  public static class Builder {
    private String value;
    private String tenantId;

    public Builder value(String value) {
      this.value = value;
      return this;
    }

    public Builder tenantId(String tenantId) {
      this.tenantId = tenantId;
      return this;
    }

    public EntityFilter build() {
      return new EntityFilter(this);
    }
  }

  public String getValue() {
    return value;
  }

  public String getTenantId() {
    return tenantId;
  }

  @Override
  public String toString() {
    return value;
  }
}
