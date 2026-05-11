package it.pagopa.selfcare.external_api.model.user;

import lombok.Data;

import java.util.List;

@Data
public class ProductInfoDetails {

    private String id;
    private String title;
    private List<RoleInfo> roleInfos;

}
