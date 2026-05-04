package it.pagopa.selfcare.mscore.connector.dao.model;

import lombok.Data;

import java.util.List;

@Data
public class DelegationPageEntity {

    private List<PageInfo> pageInfo;
    private List<DelegationEntity> delegations;

    @Data
    public static class PageInfo {
        private Long totalElements;
    }

}
