package it.pagopa.selfcare.dashboard.model.delegation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageInfo {
    private long pageSize;
    private long pageNo;
    private long totalElements;
    private long totalPages;
}
