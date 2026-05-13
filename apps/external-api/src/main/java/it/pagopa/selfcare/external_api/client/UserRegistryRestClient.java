package it.pagopa.selfcare.external_api.client;

import it.pagopa.selfcare.external_api.client.config.UserRegistryRestClientConfig;
import it.pagopa.selfcare.external_api.model.user.MutableUserFieldsDto;
import it.pagopa.selfcare.external_api.model.user.SaveUserDto;
import it.pagopa.selfcare.external_api.model.user.User;
import it.pagopa.selfcare.external_api.model.user.UserId;
import it.pagopa.selfcare.external_api.model.user_registry.EmbeddedExternalId;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.UUID;

@FeignClient(name = "${rest-client.user-registry.serviceCode}", url = "${rest-client.user-registry.base-url}", configuration = UserRegistryRestClientConfig.class)
public interface UserRegistryRestClient {

    @GetMapping(value = "${rest-client.user-registry.getUserByInternalId.path}")
    @ResponseBody
    User getUserByInternalId(@PathVariable("id") UUID id,
                             @RequestParam(value = "fl") EnumSet<User.Fields> fieldList);

    @PostMapping(value = "${rest-client.user-registry.getUserByExternalId.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    User search(@RequestBody EmbeddedExternalId externalId,
                @RequestParam(value = "fl") EnumSet<User.Fields> fields);

    @PatchMapping(value = "${rest-client.user-registry.patchUser.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void patchUser(@PathVariable("id") UUID id,
                   @RequestBody MutableUserFieldsDto request);

    @PatchMapping(value = "${rest-client.user-registry.saveUser.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    UserId saveUser(@RequestBody SaveUserDto request);

}
