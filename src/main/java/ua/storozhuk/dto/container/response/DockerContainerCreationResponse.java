package ua.storozhuk.dto.container.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;

@Getter
public class DockerContainerCreationResponse {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Warnings")
    private List<String> warnings;

}
