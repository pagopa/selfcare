package it.pagopa.selfcare.onboarding.entity;

import it.pagopa.selfcare.product.entity.AttachmentTemplate;

public class OnboardingAttachment {
  private Onboarding onboarding;
  private AttachmentTemplate attachment;

  public OnboardingAttachment() {}

  private OnboardingAttachment(OnboardingAttachment.Builder builder) {
    this.onboarding = builder.onboarding;
    this.attachment = builder.attachment;
  }

  public static OnboardingAttachment.Builder builder() {
    return new OnboardingAttachment.Builder();
  }

  public static class Builder {
    private Onboarding onboarding;
    private AttachmentTemplate attachment;

    public OnboardingAttachment.Builder onboarding(Onboarding onboarding) {
      this.onboarding = onboarding;
      return this;
    }

    public OnboardingAttachment.Builder attachment(AttachmentTemplate attachment) {
      this.attachment = attachment;
      return this;
    }

    public OnboardingAttachment build() {
      return new OnboardingAttachment(this);
    }
  }

  public AttachmentTemplate getAttachment() {
    return attachment;
  }

  public void setAttachment(AttachmentTemplate attachment) {
    this.attachment = attachment;
  }

  public Onboarding getOnboarding() {
    return onboarding;
  }

  public void setOnboarding(Onboarding onboarding) {
    this.onboarding = onboarding;
  }
}
