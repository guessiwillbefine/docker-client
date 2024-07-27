package ua.storozhuk.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import ua.storozhuk.config.properties.DockerClientConfigurationSource;
import ua.storozhuk.dto.container.request.DockerContainerCreationDto;
import ua.storozhuk.dto.container.response.DockerContainerCreationResponse;
import ua.storozhuk.dto.container.response.DockerContainerDto;
import ua.storozhuk.dto.image.response.DockerImageDto;
import ua.storozhuk.dto.image.response.DockerPrunedDto;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


//TODO refactor this sh....
@Slf4j
@Service
public class DockerClientImpl implements DockerClient {

    private final String LABEL_VALUE = "label=worker=true";

    private final String DOCKER_ADDRESS;

    private static final OkHttpClient client;

    private static final ObjectMapper objectMapper;

    static {

        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    public DockerClientImpl(DockerClientConfigurationSource configurationSource) {
        //TODO move to postconstruct
        DOCKER_ADDRESS = configurationSource.getHost() + "/" + configurationSource.getVersion() + "/";
    }


    @Override
    public List<DockerImageDto> getAllImages() {
        Response response = null;
        //TODO Response implements closeable, need to remove closeIfNeeded
        try {
            Request request = new Request.Builder().url(DOCKER_ADDRESS + "images/json")
                    .get()
                    .build();
            response = client.newCall(request).execute();
            return readResponseBody(response, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Something went wrong: ", e);
        } finally {
            closeIfNeeded(response);
        }
        return null;
    }

    @Override
    public List<DockerContainerDto> getAllContainers() {
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .url(DOCKER_ADDRESS + "containers/json")
                    .get()
                    .build();
            response = client.newCall(request).execute();
            return readResponseBody(response, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Something went wrong: ", e);
        } finally {
            closeIfNeeded(response);
        }
        return null;
    }

    @Override
    public DockerContainerCreationResponse createContainer(String containerName, DockerContainerCreationDto creationDto) {
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .header("Content-Type", "application/json")
                    .post(requestBodyFrom(creationDto))
                    .url(
                            withRequestParams(
                                    DOCKER_ADDRESS + "containers/create",
                                    Map.of("name", containerName))
                    )
                    .build();
            response = client.newCall(request).execute();
            if (response.code() == 409) {
                log.warn("Container already exists");
                return null;
            }
            return readResponseBody(response, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Something went wrong:", e);
        } finally {
            closeIfNeeded(response);
        }
        return null;
    }

    @Override
    public DockerPrunedDto pruneImages() {
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .post(RequestBody.create(new byte[]{}))
                    .url(DOCKER_ADDRESS + "images/prune")
                    .build();
            response = client.newCall(request).execute();
            if (response.code() == 409) {
                log.warn("Container already exists");
                return null;
            }
            return readResponseBody(response, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Something went wrong:", e);
        } finally {
            closeIfNeeded(response);
        }
        return null;
    }

    @Override
    public String buildImage(String pathToProject, String containerName) {
        Response response = null;
        try {
            /*
              need to think where I will store code samples and how they will reach this client
              for now I will use this way, just to test if it works in general
             */
            File file = new File(pathToProject);

            RequestBody requestBody = RequestBody.create(file, MediaType.parse("application/tar"));

            Request request = new Request.Builder()
                    .post(requestBody)
                    .url(DOCKER_ADDRESS + "build")
                    .build();
            response = client.newCall(request).execute();
            List<Map.Entry<String, Object>> entries = processStreamingData(response.body().string());
            Object aux = ((Map.Entry<Object, Object>) ((List) entries.stream().filter(entry -> entry.getKey().equals("aux")).findFirst().orElseThrow().getValue()).stream().findFirst().orElseThrow()).getValue();
            entries.forEach(System.out::println);
            return (String) aux;
        } catch (IOException e) {
            log.error("Something went wrong:", e);
            return null;
        } finally {
            closeIfNeeded(response);
        }
    }

    @Override
    public void startContainer(String id) {
        Response response = null;
        try {
            Request request = new Request.Builder()
                    .url(DOCKER_ADDRESS + "containers/" + id + "/start")
                    .post(RequestBody.create(new byte[]{}))
                    .build();
            response = client.newCall(request).execute();
        } catch (IOException e) {
            log.error("Something went wrong:", e);
        } finally {
            closeIfNeeded(response);
        }


    }

    @Override
    public boolean stopContainer(String id) {
        Request request = new Request.Builder()
                .url(DOCKER_ADDRESS + "containers/" + id + "/stop")
                .post(RequestBody.create(new byte[]{}))
                .build();
        try (Response response = client.newCall(request).execute()) {
            return switch (response.code()) {
                case 200, 304 -> true;
                case 404 -> {
                    log.warn("Container wasn't found");
                    yield false;
                }
                default -> throw new IllegalStateException("Unexpected value: " + response.code());
            };
        } catch (IOException e) {
            log.error("Something went wrong:", e);
        }
        return false;
    }

    @Override
    public void deleteStoppedContainers() {
        Request request = new Request.Builder()
                .url(DOCKER_ADDRESS + "containers/prune?" + LABEL_VALUE)
                .post(RequestBody.create(new byte[]{}))
                .build();
        try(Response response = client.newCall(request).execute()) {
            //todo
        } catch (IOException e) {
            log.error("Something went wrong:", e);
        }
    }

    private <T> T readResponseBody(Response response, TypeReference<T> type) {
        try {
            return objectMapper.readValue(response.body().string(), type);
        } catch (IOException e) {
            log.error("Exception occurred during getting response body", e);
            return null;
        }
    }

    private <T> T readResponseBodyString(String data, TypeReference<T> type) {
        try {
            return objectMapper.readValue(data, type);
        } catch (IOException e) {
            log.error("Exception occurred during getting response body", e);
            return null;
        }
    }

    private void closeIfNeeded(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException e) {
            log.error("Error during closing connection: ", e);
        }
    }

    private String withRequestParams(String url, Map<String, String> params) {
        return url + "?" + params.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((k1, k2) -> k1 + "&" + k2)
                .orElse("");
    }

    private RequestBody requestBodyFrom(Object object) {
        try {
            return RequestBody.create(objectMapper.writeValueAsBytes(object));
        } catch (JsonProcessingException e) {
            log.error("Error during creating json from " + object.getClass().getSimpleName(), e);
            return RequestBody.create(new byte[]{});
        }
    }

    private List<Map.Entry<String, Object>> processStreamingData(String data) {
        String[] split = data.split("\n");
        List<Map.Entry<String, Object>> resultList = new ArrayList<>();

        for (String jsonObject : split) {
            try {
                // Преобразуем строку JSON в JsonNode
                JsonNode jsonNode = objectMapper.readTree(jsonObject);

                // Итерируем по ключам и значениям и добавляем их в список
                Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    resultList.add(new AbstractMap.SimpleEntry<>(field.getKey(), parseJsonNode(field.getValue())));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resultList;
    }


    private static Object parseJsonNode(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            List<Map.Entry<String, Object>> map = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                map.add(new AbstractMap.SimpleEntry<>(field.getKey(), parseJsonNode(field.getValue())));
            }
            return map;
        } else if (jsonNode.isArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonNode arrayElement : jsonNode) {
                list.add(parseJsonNode(arrayElement));
            }
            return list;
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
}
