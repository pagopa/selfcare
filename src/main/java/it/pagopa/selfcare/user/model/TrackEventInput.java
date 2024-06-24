package it.pagopa.selfcare.user.model;


import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class TrackEventInput {

    private String documentKey;
    private String userId;
    private String productId;

    private String institutionId;

    private String exception;
}
