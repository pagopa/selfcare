package it.pagopa.selfcare.iam.service;

import io.smallrye.mutiny.Uni;

public interface IamService {
    Uni<String> ping();
}
