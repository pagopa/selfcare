package it.pagopa.selfcare.mscore.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CertifiedField<T> {

    private Certification certification;
    private T value;

}
