package it.pagopa.selfcare.mscore.connector.dao;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;

public class CriteriaBuilder {

    private Criteria criteria;
    private boolean first;

    private CriteriaBuilder() {
        criteria = new Criteria();
        first = true;
    }

    public static CriteriaBuilder builder() {
        return new CriteriaBuilder();
    }

    public Criteria build() {
        return criteria;
    }

    public CriteriaBuilder inIfNotEmpty(@NonNull String key, @Nullable List<?> value) {
        if (value != null && !value.isEmpty()) {
            if (first) {
                criteria = Criteria.where(key).in(value);
                first = false;
            } else {
                criteria = criteria.and(key).in(value);
            }
        }
        return this;
    }

    public CriteriaBuilder isIfNotNull(@NonNull String key, @Nullable Object value) {
        if (value != null) {
            if (first) {
                criteria = Criteria.where(key).is(value);
                first = false;
            } else {
                criteria = criteria.and(key).is(value);
            }
        }
        return this;
    }
}
