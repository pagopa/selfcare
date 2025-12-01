package it.pagopa.selfcare.product.model.enums;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum InstitutionType {
    PA,
    PG,
    GSP,
    SA,
    PT,
    SCP,
    PSP,
    AS,
    REC,
    CON,
    PRV,
    PRV_PF,
    GPU,
    SCEC,
    DEFAULT;

    public static InstitutionType from(String value) {
        if (value == null || value.isBlank()) {
            log.debug("InstitutionType is null or blank, set DEFAULT");
            return DEFAULT;
        }

        for (InstitutionType currentInstitutionType : values()) {
            if (currentInstitutionType.name().equalsIgnoreCase(value)) {
                log.debug("InstitutionType matched: {}", currentInstitutionType.name());
                return currentInstitutionType;
            }
        }

        log.warn("Unknown InstitutionType '{}', using DEFAULT", value);
        return DEFAULT;
    }
}
