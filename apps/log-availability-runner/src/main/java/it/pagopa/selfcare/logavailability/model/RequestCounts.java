package it.pagopa.selfcare.logavailability.model;

public record RequestCounts(long countLt500, long countGte500) {

    public RequestCounts {
        if (countLt500 < 0 || countGte500 < 0) {
            throw new IllegalArgumentException("Counts must not be negative");
        }
    }
}
