package it.pagopa.selfcare.dashboard.model.user;

import lombok.Data;

@Data
public class SaveUserDto extends MutableUserFieldsDto {

    private String fiscalCode;

}
