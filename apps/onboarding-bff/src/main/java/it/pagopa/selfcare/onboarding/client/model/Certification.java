package it.pagopa.selfcare.onboarding.client.model;

public enum Certification {
    NONE, SPID;

    public static boolean isCertified(Certification certification) {
        return certification != null && !Certification.NONE.equals(certification);
    }

}
