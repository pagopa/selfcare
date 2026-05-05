package it.pagopa.selfcare.external_api.mapper;

import it.pagopa.selfcare.external_api.model.token.Token;
import it.pagopa.selfcare.external_api.model.token.TokenOnboardedUsers;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.OnboardingGet;
import it.pagopa.selfcare.onboarding.generated.openapi.v1.dto.TokenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Mapper(componentModel = "spring", uses = TokenUserMapper.class)
public interface TokenMapper {

    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(source = "closedAt", target = "closedAt", qualifiedByName = "toOffsetDateTime")
    Token toEntity(TokenResponse response);

    @Mapping(target = "institutionId", source = "institution.id")
    @Mapping(target = "institutionUpdate", source = "institution")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(target = "expiringDate", source = "expiringDate", qualifiedByName = "toOffsetDateTime")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(target = "activatedAt", source = "activatedAt", qualifiedByName = "toOffsetDateTime")
    @Mapping(target = "institutionUpdate.payment", source = "payment")
    TokenOnboardedUsers toEntity(OnboardingGet response);

    @Named("toOffsetDateTime")
    default OffsetDateTime map(LocalDateTime localDateTime) {
        return Optional.ofNullable(localDateTime)
                .map(time -> time.atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .orElse(null);
    }
}
