package ua.storozhuk.dto.container.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class DockerContainerDto {

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Names")
    private List<String> names;

    @JsonProperty("Image")
    private String image;

    @JsonProperty("ImageID")
    private String imageId;

    @JsonProperty("Command")
    private String command;

    @JsonProperty("Created")
    private long created;

    @JsonProperty("Ports")
    private List<Port> ports;

    @JsonProperty("Labels")
    private Map<String, String> labels;

    @Getter
    public static class Port {

        @JsonProperty("IP")
        private String IP;

        @JsonProperty("PrivatePort")
        private String privatePort;

        @JsonProperty("PublicPort")
        private String publicPort;

        @JsonProperty("Type")
        private String type;
    }

}
