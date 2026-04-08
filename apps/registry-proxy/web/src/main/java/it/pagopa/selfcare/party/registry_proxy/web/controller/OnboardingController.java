package it.pagopa.selfcare.party.registry_proxy.web.controller;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.pagopa.selfcare.party.registry_proxy.core.SearchService;
import it.pagopa.selfcare.party.registry_proxy.web.model.OnboardingIndexResource;
import it.pagopa.selfcare.party.registry_proxy.web.model.mapper.OnboardingMapper;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/onboarding")
@Tag(name = "onboarding")
public class OnboardingController {

    private final SearchService searchService;
    private final OnboardingMapper onboardingMapper;

    public OnboardingController(SearchService searchService, OnboardingMapper onboardingMapper) {
        this.searchService = searchService;
        this.onboardingMapper = onboardingMapper;
    }

    @PostMapping("/update-index")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Onboarding index updated successfully")
    public ResponseEntity<Void> updateOnboardingIndex(@RequestBody @Valid OnboardingIndexResource onboardingIndexResource) {
        searchService.indexOnboarding(onboardingMapper.toOnboardingIndex(onboardingIndexResource));
        return ResponseEntity.noContent().build();
    }

}
