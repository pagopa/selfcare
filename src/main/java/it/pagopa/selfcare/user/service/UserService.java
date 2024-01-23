package it.pagopa.selfcare.user.service;

import io.smallrye.mutiny.Multi;
import it.pagopa.selfcare.user.controller.response.UserProductResponse;

import io.smallrye.mutiny.Uni;

import java.util.List;

public interface UserService {
    Uni<List<String>> getUsersEmails(String institutionId, String productId);
    Multi<UserProductResponse> getUserProductsByInstitution(String institutionId);
}
