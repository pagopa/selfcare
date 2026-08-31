package it.pagopa.selfcare.commons.base;

public enum TargetEnvironment {
    DEV, UAT, PROD;

    private static TargetEnvironment currentEnv;

    public static TargetEnvironment getCurrent() {
        TargetEnvironment currentEnv;
        try {
            currentEnv = valueOf(System.getenv("ENV_TARGET"));
        } catch (RuntimeException e) {
            currentEnv = null;
        }
        return currentEnv;
    }
}
