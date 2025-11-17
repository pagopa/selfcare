package it.pagopa.selfcare.product.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.*;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;

import java.io.StringReader;

@Singleton
@RequiredArgsConstructor(onConstructor_ = {@Inject})
public class JsonUtils {

    private final ObjectMapper objectMapper;

    public JsonNode valueToTree(Object source) {
        return objectMapper.valueToTree(source);
    }

    public <T> T treeToValue(JsonNode node, Class<T> type) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid JSON payload", e);
        }
    }

    public JsonNode applyMergePatch(JsonMergePatch patch, JsonNode target) {
        try {
            String targetStr = objectMapper.writeValueAsString(target);

            try (StringReader sr = new StringReader(targetStr);
                 JsonReader reader = Json.createReader(sr)) {

                JsonStructure targetJson = reader.read();
                JsonValue patched = patch.apply(targetJson);
                return objectMapper.readTree(patched.toString());
            }
        } catch (Exception e) {
            throw new BadRequestException("Cannot apply merge patch", e);
        }
    }

    public <T> T mergePatch(JsonMergePatch patch, T current, Class<T> type) {
        JsonNode mergedNode = applyMergePatch(patch, valueToTree(current));
        return treeToValue(mergedNode, type);
    }

    public JsonMergePatch toMergePatch(JsonValue jsonBody) {
        if (jsonBody == null) {
            throw new BadRequestException("Missing request param to update");
        }
        if (jsonBody.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new BadRequestException("Invalid merge patch document");
        }
        JsonObject obj = jsonBody.asJsonObject();
        if (obj.isEmpty()) {
            throw new BadRequestException("Missing request param to update");
        }
        try {
            return Json.createMergePatch(obj);
        } catch (Exception e) {
            throw new BadRequestException("Invalid merge patch document", e);
        }
    }

}
