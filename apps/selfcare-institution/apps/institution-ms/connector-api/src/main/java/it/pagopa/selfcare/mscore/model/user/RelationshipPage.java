package it.pagopa.selfcare.mscore.model.user;

import lombok.Data;

import java.util.List;

@Data
public class RelationshipPage {
    private Integer total;
    private List<RelationshipPageElement> data;
}
