package it.pagopa.selfcare.user.util;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@RequiredArgsConstructor
@Slf4j
public class UserUtils {

    @SafeVarargs
    public final Map<String, Object> retrieveMapForFilter(Map<String, Object>... maps) {
        Map<String, Object> map = new HashMap<>();
        Arrays.stream(maps).forEach(map::putAll);
        return map;
    }
}