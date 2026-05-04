package it.pagopa.selfcare.dashboard.client;

import it.pagopa.selfcare.dashboard.config.restclient.UserRegistryRestClientConfig;
import it.pagopa.selfcare.dashboard.model.user.MutableUserFieldsDto;
import it.pagopa.selfcare.dashboard.model.user.SaveUserDto;
import it.pagopa.selfcare.dashboard.model.user.User;
import it.pagopa.selfcare.dashboard.model.user.UserId;
import it.pagopa.selfcare.dashboard.model.user_registry.EmbeddedExternalId;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.UUID;

@FeignClient(name = "${rest-client.user-registry.serviceCode}", url = "${rest-client.user-registry.base-url}", configuration = UserRegistryRestClientConfig.class)
public interface UserRegistryRestClient {

    @PostMapping(value = "${rest-client.user-registry.getUserByExternalId.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    User search(@RequestBody EmbeddedExternalId externalId,
                @RequestParam(value = "fl") EnumSet<User.Fields> fields);

    @PatchMapping(value = "${rest-client.user-registry.patchUser.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void patchUser(@PathVariable("id") UUID id,
                   @RequestBody MutableUserFieldsDto request);

    @GetMapping(value = "${rest-client.user-registry.getUserByInternalId.path}")
    @ResponseBody
    User getUserByInternalId(@PathVariable("id") UUID id,
                             @RequestParam(value = "fl") EnumSet<User.Fields> fieldList);

    @PatchMapping(value = "${rest-client.user-registry.saveUser.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    UserId saveUser(@RequestBody SaveUserDto request);

    @DeleteMapping(value = "${rest-client.user-registry.deleteUserById.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    void deleteById(@PathVariable("id") UUID id);

}
