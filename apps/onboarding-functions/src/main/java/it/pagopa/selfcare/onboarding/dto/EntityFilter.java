package it.pagopa.selfcare.onboarding.dto;

public class EntityFilter {
  private String value;

  public EntityFilter() {
  }

  private EntityFilter(Builder builder) {
    this.value = builder.value;
  }

  public static EntityFilter.Builder builder() {
    return new EntityFilter.Builder();
  }

  public static class Builder {
    private String value;

    public Builder value(String value) {
      this.value = value;
      return this;
    }

    public EntityFilter build() {
      return new EntityFilter(this);
    }
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
