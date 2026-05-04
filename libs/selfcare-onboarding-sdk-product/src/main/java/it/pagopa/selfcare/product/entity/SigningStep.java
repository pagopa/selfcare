package it.pagopa.selfcare.product.entity;

public class SigningStep {
    /**
     * Progression number of the step (1, 2, 3, ...).
     * Determines the order in which signatures must be applied.
     */
    private int order;

    private String label;

    /**
     * Indicates whether this is the last step in the signing flow.
     * Only the step with isFinal=true causes the transition to COMPLETED.
     */
    private boolean isFinal;

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }
}

