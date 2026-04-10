package it.pagopa.selfcare.onboarding.connector.rest.client;

import it.pagopa.selfcare.onboarding.connector.model.UploadedFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.openapi.quarkus.onboarding_json.api.OnboardingControllerApi;
import org.openapi.quarkus.onboarding_json.model.CheckManagerRequest;
import org.openapi.quarkus.onboarding_json.model.CheckManagerResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingDefaultRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingGet;
import org.openapi.quarkus.onboarding_json.model.OnboardingGetResponse;
import org.openapi.quarkus.onboarding_json.model.OnboardingPaRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingPgRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingPspRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingUserPgRequest;
import org.openapi.quarkus.onboarding_json.model.OnboardingUserRequest;
import org.openapi.quarkus.onboarding_json.model.ReasonRequest;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "onboarding_json")
public interface MsOnboardingApiClient extends OnboardingControllerApi {

    default void _onboardingPa(OnboardingPaRequest request) {
        onboardingPa(request).await().indefinitely();
    }

    default void _onboardingPsp(OnboardingPspRequest request) {
        onboardingPsp(request).await().indefinitely();
    }

    default void _onboarding(OnboardingDefaultRequest request) {
        onboarding(request).await().indefinitely();
    }

    default void _onboardingUsers(OnboardingUserRequest request) {
        onboardingUsers(request).await().indefinitely();
    }

    default void _onboardingUsersAggregator(OnboardingUserRequest request) {
        onboardingUsersAggregator(request).await().indefinitely();
    }

    default void _onboardingPgCompletion(OnboardingPgRequest request) {
        onboardingPgCompletion(request).await().indefinitely();
    }

    default void _onboardingPaAggregation(OnboardingPaRequest request) {
        onboardingPaAggregation(request).await().indefinitely();
    }

    default void _onboardingUsersPg(OnboardingUserPgRequest request) {
        onboardingUsersPg(request).await().indefinitely();
    }

    default void _completeOnboardingUser(String onboardingId, UploadedFile contract) {
        OnboardingControllerApi.CompleteOnboardingUserMultipartForm form =
            new OnboardingControllerApi.CompleteOnboardingUserMultipartForm();
        form.contract = toTempFile(contract);
        completeOnboardingUser(form, onboardingId).await().indefinitely();
    }

    default void _approve(String onboardingId) {
        approve(onboardingId).await().indefinitely();
    }

    default void _delete(String onboardingId, ReasonRequest reasonRequest) {
        delete(onboardingId, reasonRequest).await().indefinitely();
    }

    default OnboardingGet _getById(String onboardingId) {
        return getById(onboardingId).await().indefinitely();
    }

    default OnboardingGet _getByIdWithUserInfo(String onboardingId) {
        return getByIdWithUserInfo(onboardingId).await().indefinitely();
    }

    default OnboardingGet _getOnboardingPending(String onboardingId) {
        return getOnboardingPending(onboardingId).await().indefinitely();
    }

    default CheckManagerResponse _checkManager(CheckManagerRequest request) {
        return checkManager(request).await().indefinitely();
    }

    default void _verifyOnboardingInfoByFilters(
        String institutionType,
        String origin,
        String originId,
        String productId,
        String subunitCode,
        String taxCode
    ) {
        verifyOnboardingInfoByFilters(institutionType, origin, originId, productId, subunitCode, taxCode)
            .await().indefinitely();
    }

    default OnboardingGetResponse _getOnboardingWithFilter(
        String from,
        String institutionId,
        String onboardingId,
        Integer page,
        String productId,
        java.util.List<String> productIds,
        Integer size,
        Boolean skipPagination,
        String status,
        String subunitCode,
        String taxCode,
        String to,
        String userId
    ) {
        return getOnboardingWithFilter(
            from,
            institutionId,
            onboardingId,
            page,
            productId,
            productIds,
            size,
            skipPagination,
            status,
            subunitCode,
            taxCode,
            to,
            userId
        ).await().indefinitely();
    }

    private static File toTempFile(UploadedFile uploadedFile) {
        try {
            String fileName = uploadedFile.fileName();
            String suffix = fileName == null ? ".bin" : "-" + fileName;
            File file = Files.createTempFile("onboarding-", suffix).toFile();
            Files.write(file.toPath(), uploadedFile.content());
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot convert multipart file", e);
        }
    }
}
