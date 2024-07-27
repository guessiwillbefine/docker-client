package ua.storozhuk.dto.container.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import ua.storozhuk.dto.common.HostConfig;

import java.util.Map;

@Getter
@Builder
public class DockerContainerCreationDto {

    @JsonProperty("Image")
    private String image;

    @JsonProperty("HostConfig")
    private HostConfig hostConfig;

    @JsonProperty("Labels")
    private Map<String, String> Labels;

}
