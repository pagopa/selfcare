package it.pagopa.selfcare.user.service;


import io.smallrye.mutiny.Uni;

import java.util.List;

public interface UserService {
    Uni<List<String>> getUsersEmails(String institutionId, String productId);
}
