package ua.storozhuk.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
import ua.storozhuk.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

//TODO refactor this sh....
//TODO also need some logic to attempt to connect after bean creation
@Slf4j
@Service
public class DockerClientImpl implements DockerClient {

    //TODO move label to app that will use this client
    private final String LABEL_VALUE = "label=worker=true";

    /** Key in stream response data where we will be placed id of created container */
    private final String ID_KEY = "aux";

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

    @PostConstruct
    private void ping() throws IOException {
        Request request = new Request.Builder()
                .url(DOCKER_ADDRESS + "images/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            log.info("Docker API initialized and connected successfully");
        } catch (IOException e) {
            log.error("Docker API is not accessible: ", e);
            throw e;
        }
    }

    @Override
    public List<DockerImageDto> getAllImages() {
        Request request = new Request.Builder()
                .url(DOCKER_ADDRESS + "images/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {

            return readResponseBody(response, new TypeReference<>() {});

        } catch (IOException e) {
            log.error("Something went wrong: ", e);
        }
        return null;
    }

    @Override
    public List<DockerContainerDto> getAllContainers() {
        Request request = new Request.Builder()
                .url(DOCKER_ADDRESS + "containers/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {

            return readResponseBody(response, new TypeReference<>() {});

        } catch (IOException e) {
            log.error("Something went wrong: ", e);
        }
        return null;
    }

    @Override
    public DockerContainerCreationResponse createContainer(String containerName, DockerContainerCreationDto creationDto) {
        Request request = new Request.Builder()
                .header("Content-Type", "application/json")
                .post(requestBodyFrom(creationDto))
                .url(
                        withRequestParams(
                                DOCKER_ADDRESS + "containers/create",
                                Map.of("name", containerName))
                )
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 409) {
                log.warn("Container already exists");
                return null;
            }
            return readResponseBody(response, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.error("Something went wrong:", e);
        }
        return null;
    }

    @Override
    public DockerPrunedDto pruneImages() {
        Request request = new Request.Builder()
                .post(RequestBody.create(new byte[]{}))
                .url(DOCKER_ADDRESS + "images/prune")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 409) {
                log.warn("Container already exists");
                return null;
            }
            return readResponseBody(response, new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Something went wrong:", e);
        }
        return null;
    }

    @Override
    public String buildImage(String pathToProject, String containerName) {

        File file = new File(pathToProject);

        RequestBody requestBody = RequestBody.create(file, MediaType.parse("application/tar"));
        Request request = new Request.Builder()
                .post(requestBody)
                .url(DOCKER_ADDRESS + "build")
                .build();

        try (Response response = client.newCall(request).execute()) {
            /*
              need to think where I will store code samples and how they will reach this client
              for now I will use this way, just to test if it works in general
             */
            List<Map.Entry<String, Object>> entries = JsonUtils.parseStreamingData(response.body().string());
            Object aux = entries.stream()
                    .filter(entry -> ID_KEY.equals(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .filter(value -> value instanceof List)
                    .map(value -> ((List<?>) value).stream().findFirst())
                    .flatMap(Optional::stream)
                    .map(entry -> ((Map.Entry<?, ?>) entry).getValue())
                    .findFirst()
                    .orElseThrow();

            entries.forEach(System.out::println);
            return (String) aux;
        } catch (IOException e) {
            log.error("Something went wrong:", e);
            return null;
        }
    }

    @Override
    public void startContainer(String id) {
        Request request = new Request.Builder()
                .url(DOCKER_ADDRESS + "containers/" + id + "/start")
                .post(RequestBody.create(new byte[]{}))
                .build();
        try (Response ignored = client.newCall(request).execute()) {
            log.debug(String.format("container %s started", id));
        } catch (IOException e) {
            log.error("Something went wrong:", e);
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
        try(Response ignored = client.newCall(request).execute()) {
            //todo read response and log deleted ids
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

}
