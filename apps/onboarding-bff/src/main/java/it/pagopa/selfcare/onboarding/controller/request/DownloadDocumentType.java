package it.pagopa.selfcare.onboarding.controller.request;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Type of document to download.")
public enum DownloadDocumentType {

    /** Signed contract associated with the onboarding. Requires only the onboardingId. */
    CONTRACT_SIGNED,

    /** Attachment associated with the onboarding. Requires the additional {@code name} query parameter. */
    ATTACHMENT
}
