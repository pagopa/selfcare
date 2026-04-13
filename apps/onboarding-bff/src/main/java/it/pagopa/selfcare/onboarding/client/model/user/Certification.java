package it.pagopa.selfcare.onboarding.client.model.user;

public enum Certification {
    NONE, SPID;

    public static boolean isCertified(Certification certification) {
        return certification != null && !Certification.NONE.equals(certification);
    }

}
