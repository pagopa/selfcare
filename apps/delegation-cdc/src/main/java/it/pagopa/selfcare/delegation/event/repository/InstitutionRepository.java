package it.pagopa.selfcare.delegation.event.repository;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.delegation.event.entity.Institution;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class InstitutionRepository {

    public Uni<Institution> findInstitutionById(String institutionId) {
        return Institution.findByIdOptional(institutionId)
                .flatMap(optionalEntity -> {
                    if (optionalEntity.isPresent()) {
                        Institution institution = (Institution) optionalEntity.get();
                        return Uni.createFrom().item(institution);
                    } else {
                        return Uni.createFrom().nullItem();
                    }
                });
    }

}

