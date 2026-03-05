package it.pagopa.selfcare.onboarding.entity;

public class GPUData extends BusinessData {

    private boolean manager;
    private boolean managerAuthorized;
    private boolean managerEligible;
    private boolean managerProsecution;
    private boolean institutionCourtMeasures;

    public boolean isManager() {
        return manager;
    }

    public void setManager(boolean manager) {
        this.manager = manager;
    }

    public boolean isManagerAuthorized() {
        return managerAuthorized;
    }

    public void setManagerAuthorized(boolean managerAuthorized) {
        this.managerAuthorized = managerAuthorized;
    }

    public boolean isManagerEligible() {
        return managerEligible;
    }

    public void setManagerEligible(boolean managerEligible) {
        this.managerEligible = managerEligible;
    }

    public boolean isManagerProsecution() {
        return managerProsecution;
    }

    public void setManagerProsecution(boolean managerProsecution) {
        this.managerProsecution = managerProsecution;
    }

    public boolean isInstitutionCourtMeasures() {
        return institutionCourtMeasures;
    }

    public void setInstitutionCourtMeasures(boolean institutionCourtMeasures) {
        this.institutionCourtMeasures = institutionCourtMeasures;
    }
}

