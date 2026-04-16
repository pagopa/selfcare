package it.pagopa.selfcare.mscore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RootParent {
    private String id;
    private String originId;
    private String description;
}
