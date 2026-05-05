package it.pagopa.selfcare.external_api.model.user;

import lombok.Data;

@Data
public class SaveUserDto extends MutableUserFieldsDto {
    private String fiscalCode;
}
