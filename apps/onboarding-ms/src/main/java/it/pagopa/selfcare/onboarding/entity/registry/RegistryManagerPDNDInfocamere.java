package it.pagopa.selfcare.onboarding.entity.registry;

import io.smallrye.mutiny.Uni;
import it.pagopa.selfcare.onboarding.common.InstitutionType;
import it.pagopa.selfcare.onboarding.entity.Institution;
import it.pagopa.selfcare.onboarding.entity.registry.client.ClientRegistryPDNDInfocamere;
import it.pagopa.selfcare.onboarding.entity.Onboarding;
import it.pagopa.selfcare.onboarding.exception.InvalidRequestException;
import it.pagopa.selfcare.product.entity.Product;
import jakarta.ws.rs.WebApplicationException;
import org.openapi.quarkus.party_registry_proxy_json.api.InfocamerePdndApi;
import org.openapi.quarkus.party_registry_proxy_json.api.PdndVisuraInfoCamereControllerApi;
import org.openapi.quarkus.party_registry_proxy_json.model.PDNDBusinessResource;
import org.openapi.quarkus.user_registry_json.api.UserApi;
import org.openapi.quarkus.user_registry_json.model.SaveUserDto;
import org.openapi.quarkus.user_registry_json.model.UserSearchDto;

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.selfcare.onboarding.common.ProductId.PROD_IDPAY_MERCHANT;

public class RegistryManagerPDNDInfocamere extends ClientRegistryPDNDInfocamere {

    public static final String USERS_FIELD_TAXCODE = "fiscalCode";

    private final UserApi userRegistryApi;
    private final Optional<String> allowedAtecoCodes;
    private final PdndVisuraInfoCamereControllerApi pdndVisuraInfoCamereControllerApi;

    public RegistryManagerPDNDInfocamere(Onboarding onboarding, InfocamerePdndApi infocamerePdndApi, UserApi userRegistryApi,
                                         Optional<String> allowedAtecoCodes, PdndVisuraInfoCamereControllerApi pdndVisuraInfoCamereControllerApi) {
        super(onboarding, infocamerePdndApi);
        this.userRegistryApi = userRegistryApi;
        this.allowedAtecoCodes = allowedAtecoCodes;
        this.pdndVisuraInfoCamereControllerApi = pdndVisuraInfoCamereControllerApi;
    }

    @Override
    public Uni<Onboarding> customValidation(Product product) {
        if (isIdPayMerchantProduct(product)) {
            return validateAtecoCodes()
                    .chain(() -> isPrivatePersonInstitution()
                            ? manageTaxCode()
                            : Uni.createFrom().item(onboarding));
        }
        return Uni.createFrom().item(onboarding);
    }

    private boolean isIdPayMerchantProduct(Product product) {
        return Objects.nonNull(product) && PROD_IDPAY_MERCHANT.getValue().equals(product.getId());
    }

    private boolean isPrivatePersonInstitution() {
        return InstitutionType.PRV_PF.equals(onboarding.getInstitution().getInstitutionType());
    }

    private Uni<Onboarding> manageTaxCode() {
        final String fiscalCode = onboarding.getInstitution().getTaxCode();
        final UserSearchDto searchDto = new UserSearchDto().fiscalCode(fiscalCode);
        return userRegistryApi.searchUsingPOST(USERS_FIELD_TAXCODE, searchDto)
                .onItem().transform(userResource ->
                        updateOnboardingWithUserId(onboarding, userResource.getId().toString())
                )
                .onFailure(WebApplicationException.class)
                .recoverWithUni(ex ->
                        handleSearchFailure((WebApplicationException) ex, onboarding, fiscalCode)
                );
    }

    /**
     * Gestisce il fallimento della ricerca utente. Se l'errore è un 404 (Not Found),
     * procede con la creazione di un nuovo utente. Altrimenti, propaga l'errore.
     */
    private Uni<Onboarding> handleSearchFailure(WebApplicationException ex, Onboarding onboarding, String fiscalCode) {
        if (ex.getResponse().getStatus() == 404) {
            return createNewUserAndUpdateOnboarding(onboarding, fiscalCode);
        }
        return Uni.createFrom().failure(ex);
    }

    /**
     * Crea un nuovo utente tramite API e aggiorna l'oggetto onboarding con l'ID restituito.
     */
    private Uni<Onboarding> createNewUserAndUpdateOnboarding(Onboarding onboarding, String fiscalCode) {
        final SaveUserDto saveUserDto = new SaveUserDto();
        saveUserDto.setFiscalCode(fiscalCode);
        return userRegistryApi.saveUsingPATCH(saveUserDto)
                .onItem()
                .transform(userId ->
                        updateOnboardingWithUserId(onboarding, userId.getId().toString())
                );
    }

    /**
     * Metodo di utilità per aggiornare l'oggetto onboarding con un ID utente,
     * eliminando la duplicazione del codice.
     */
    private Onboarding updateOnboardingWithUserId(Onboarding onboarding, String userId) {
        onboarding.getInstitution().setTaxCode(userId);
        onboarding.getInstitution().setOriginId(userId);
        return onboarding;
    }

    /**
     * Valida che almeno uno dei codici ATECO dell'istituzione sia presente nella lista di codici ATECO consentiti.
     * @return Uni<Void> che fallisce con InvalidRequestException se la validazione fallisce
     */
    private Uni<Onboarding> validateAtecoCodes() {
        List<String> institutionAtecoCodes = onboarding.getInstitution().getAtecoCodes();

        if (Objects.isNull(institutionAtecoCodes) || institutionAtecoCodes.isEmpty()) {
            return Uni.createFrom().failure(new InvalidRequestException("Institution must have at least one ATECO code"));
        }

        List<String> allowedCodes = allowedAtecoCodes
                .filter(codes -> !codes.trim().isEmpty())
                .map(codes -> Arrays.stream(codes.split(","))
                        .map(String::trim)
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new InvalidRequestException("Allowed ATECO codes are not configured"));

        return validPdndVisuraInfocamere(pdndVisuraInfoCamereControllerApi, allowedCodes)
                .onItem().transform(isValid -> {
                    if (!isValid) {
                        throw new InvalidRequestException("Institution ATECO codes from PDND Visura are not allowed for this product");
                    }
                    return onboarding;
                });
    }

    @Override
    public Uni<Boolean> isValid() {
        /*if (!originPDNDInfocamere(onboarding, registryResource)) {
              return Uni.createFrom().failure(new InvalidRequestException("Field digitalAddress or description are not valid"));
        }*/
        return Uni.createFrom().item(true);
    }

    private Uni<Boolean> validPdndVisuraInfocamere(PdndVisuraInfoCamereControllerApi pdndVisuraInfoCamereControllerApi,
                                                   List<String> allowedCodes) {
        Institution institution = onboarding.getInstitution();
        String taxCode = institution.getTaxCode();

        return pdndVisuraInfoCamereControllerApi.institutionVisuraPdndByTaxCodeUsingGET(taxCode)
                .onItem().transform(pdndBusinessResource -> {
                    List<String> pdndAtecoCodes = pdndBusinessResource.getAtecoCodes();

                    if (pdndAtecoCodes == null || pdndAtecoCodes.isEmpty()) {
                        return false;
                    }

                    Set<String> pdndAteco = new HashSet<>(pdndAtecoCodes);

                    Set<String> institutionAtecoCodes = new HashSet<>(institution.getAtecoCodes());

                    if (!pdndAteco.equals(institutionAtecoCodes)) {
                        throw new InvalidRequestException("Institution ATECO codes from request doesn't match with ATECO codes from PDND Visura");
                    }

                    return institutionAtecoCodes.stream()
                            .map(String::trim)
                            .anyMatch(allowedCodes::contains);
                });
    }

    private boolean originPDNDInfocamere(Onboarding onboarding, PDNDBusinessResource pdndBusinessResource) {
        return onboarding.getInstitution().getDigitalAddress().equals(pdndBusinessResource.getDigitalAddress()) &&
                onboarding.getInstitution().getDescription().equals(pdndBusinessResource.getBusinessName());
    }

}