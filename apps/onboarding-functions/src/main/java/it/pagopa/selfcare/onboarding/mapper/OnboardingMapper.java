package it.pagopa.selfcare.onboarding.mapper;

import it.pagopa.selfcare.onboarding.dto.OnboardingAggregateOrchestratorInput;
import it.pagopa.selfcare.onboarding.entity.AggregateInstitution;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.entity.User;
import org.apache.commons.collections4.CollectionUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.openapi.quarkus.core_json.model.DelegationResponse;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static it.pagopa.selfcare.onboarding.common.PartyRole.ADMIN_EA;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IO;
import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_PAGOPA;

@Mapper(componentModel = "cdi")
public interface OnboardingMapper {

    @Mapping(target = "institution", expression = "java(mapInstitution(input.getAggregate(), input.getInstitution()))")
    @Mapping(target = "aggregator", source = "institution")
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
    @Mapping(target = "activatedAt", ignore = true)
    @Mapping(target = "referenceOnboardingId", source = "referenceOnboardingId")
    Onboarding mapToOnboarding(OnboardingAggregateOrchestratorInput input);

    @Mapping(target = "aggregate", expression = "java(mapFromAggregateInstitution(aggregateInstitution))")
    @Mapping(target = "users", expression = "java(mapAggregateUsers(onboarding, aggregateInstitution))")
    @Mapping(target = "referenceOnboardingId", source = "onboarding.id")
    OnboardingAggregateOrchestratorInput mapToOnboardingAggregateOrchestratorInput(Onboarding onboarding, AggregateInstitution aggregateInstitution);

    @Mapping(target = "institution", expression = "java(mapInstitutionFromDelegation(delegationResponse))")
    @Mapping(target = "delegationId", source = "delegationResponse.id")
    @Mapping(target = "users", expression = "java(mapUsers(onboarding))")
    @Mapping(target = "id", source = "onboarding.id")
    @Mapping(target = "productId", source = "onboarding.productId")
    @Mapping(target = "status", source = "onboarding.status")
    @Mapping(target = "createdAt", source = "onboarding.createdAt")
    @Mapping(target = "updatedAt", source = "onboarding.updatedAt")
    Onboarding mapToOnboardingFromDelegation(Onboarding onboarding, DelegationResponse delegationResponse);

    @Mapping(target = "id", source = "institutionId")
    @Mapping(target = "description", source = "institutionName")
    @Mapping(target = "parentDescription", source = "institutionRootName")
    Institution mapInstitutionFromDelegation(DelegationResponse delegationResponse);

    @Mapping(target = "id", source = "onboarding.id")
    @Mapping(target = "users", expression = "java(mapSingleUser(user))")
    Onboarding mapToOnboardingWithSingleUser(Onboarding onboarding, User user);

    /**
     * We need to create an explicit method to map the aggregate into the institution field of the new onboarding entity
     * because the data related to institutionType and origin must be retrieved from the aggregator,
     * which corresponds to the institution field in the input object.
     */
    @Named("mapInstitution")
    default Institution mapInstitution(Institution aggregate, Institution institution) {
        if (Objects.nonNull(institution)) {
            aggregate.setOrigin(institution.getOrigin());
            aggregate.setInstitutionType(institution.getInstitutionType());
            aggregate.setCountry(institution.getCountry());
        }
        return aggregate;
    }


    @Named("mapAggregateUsers")
    default List<User> mapAggregateUsers(Onboarding onboarding, AggregateInstitution aggregateInstitution) {
        if (Objects.nonNull(aggregateInstitution) && !CollectionUtils.isEmpty(aggregateInstitution.getUsers())) {
            return aggregateInstitution.getUsers();
        }
        if (PROD_PAGOPA.getValue().equals(onboarding.getProductId()) || PROD_IO.getValue().equals(onboarding.getProductId())) {
            return List.of();
        }
        return onboarding.getUsers();
    }

    @Named("mapAggregateUsers")
    default List<User> mapUsers(Onboarding onboarding) {
        if (PROD_PAGOPA.getValue().equals(onboarding.getProductId()) || PROD_IO.getValue().equals(onboarding.getProductId())) {
            onboarding.getUsers().forEach(user -> user.setRole(ADMIN_EA));
        }
        return onboarding.getUsers();
    }

    @Named("mapSingleUser")
    default List<User> mapSingleUser(User user) {
        return Collections.singletonList(user);
    }

    Institution mapFromAggregateInstitution(AggregateInstitution aggregateInstitution);
}
