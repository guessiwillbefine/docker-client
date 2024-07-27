package ua.storozhuk.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static List<Map.Entry<String, Object>> parseStreamingData(String data) {
        String[] split = data.split("\n");
        List<Map.Entry<String, Object>> resultList = new ArrayList<>();

        for (String jsonObject : split) {
            try {
                JsonNode jsonNode = OBJECT_MAPPER.readTree(jsonObject);

                parseJsonFields(jsonNode, resultList);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return resultList;
    }

    private static Object parseJsonNode(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            List<Map.Entry<String, Object>> map = new ArrayList<>();
            parseJsonFields(jsonNode, map);
            return map;
        } else if (jsonNode.isArray()) {
            return parseJsonArray(jsonNode);
        } else if (jsonNode.isTextual()) {
            return jsonNode.asText();
        } else if (jsonNode.isNumber()) {
            return jsonNode.numberValue();
        } else if (jsonNode.isBoolean()) {
            return jsonNode.asBoolean();
        } else {
            return null;
        }
    }

    private static List<Object> parseJsonArray(JsonNode jsonNode) {
        List<Object> list = new ArrayList<>();
        for (JsonNode arrayElement : jsonNode) {
            list.add(parseJsonNode(arrayElement));
        }
        return list;
    }

    private static void parseJsonFields(JsonNode jsonNode, List<Map.Entry<String, Object>> map) {
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            map.add(new AbstractMap.SimpleEntry<>(field.getKey(), parseJsonNode(field.getValue())));
        }
    }
}
