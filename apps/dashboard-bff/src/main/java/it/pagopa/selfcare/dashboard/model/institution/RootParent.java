package it.pagopa.selfcare.dashboard.model.institution;

import lombok.Data;

import java.io.Serializable;

@Data
public class RootParent implements Serializable {
    private String id;
    private String description;
}
