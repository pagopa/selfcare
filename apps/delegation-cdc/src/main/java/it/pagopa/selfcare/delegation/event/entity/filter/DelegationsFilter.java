package it.pagopa.selfcare.delegation.event.entity.filter;

import it.pagopa.selfcare.delegation.event.entity.DelegationsEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.selfcare.delegation.event.entity.filter.DelegationsFilter.DelegationsFieldsEnum.*;

@Builder
public class DelegationsFilter {

    private final Object productId;
    private final Object status;
    private final Object from;
    private final Object to;
    private final Object type;


    @Getter
    @AllArgsConstructor
    public enum DelegationsFieldsEnum {
        PRODUCT_ID(DelegationsEntity.Fields.productId.name()),
        STATUS(DelegationsEntity.Fields.status.name()),
        FROM(DelegationsEntity.Fields.from.name()),
        TO(DelegationsEntity.Fields.to.name()),
        TYPE(DelegationsEntity.Fields.type.name());

        private final String definition;

    }

    public Map<String, Object> constructMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(PRODUCT_ID.getDefinition(), productId);
        map.put(STATUS.getDefinition(), status);
        map.put(FROM.getDefinition(), from);
        map.put(TO.getDefinition(), to);
        map.put(TYPE.getDefinition(), type);

        map.entrySet().removeIf(e -> Objects.isNull(e.getValue()) ||
                (e.getValue() instanceof Collection && ((Collection<?>) e.getValue()).isEmpty()));

        return map;
    }
}
