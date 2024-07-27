package ua.storozhuk.dto.image.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
public class DockerImageDto {
    @JsonProperty("Containers")
    private int containers;

    @JsonProperty("Created")
    private long created;

    @JsonProperty("Id")
    private String id;

    @JsonProperty("Labels")
    private Map<String, String> labels;

    @JsonProperty("ParentId")
    private String parentId;

    @JsonProperty("RepoDigests")
    private List<String> repoDigests;

    @JsonProperty("RepoTags")
    private List<String> repoTags;

    @JsonProperty("SharedSize")
    private int sharedSize;

    @JsonProperty("Size")
    private long size;

    @JsonProperty("VirtualSize")
    private long virtualSize;
}
