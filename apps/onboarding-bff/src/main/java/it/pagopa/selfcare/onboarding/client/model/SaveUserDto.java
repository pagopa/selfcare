package it.pagopa.selfcare.onboarding.client.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class SaveUserDto extends MutableUserFieldsDto {

    private String fiscalCode;

}
