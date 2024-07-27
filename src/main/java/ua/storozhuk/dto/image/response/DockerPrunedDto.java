package ua.storozhuk.dto.image.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class DockerPrunedDto {

    @Nullable
    @JsonProperty("ImagesDeleted")
    private List<PruneResult> imagesDeleted;

    @JsonProperty("SpaceReclaimed")
    private long spaceReclaimed;

    @Getter
    public static class PruneResult {

        @JsonProperty(value = "Untagged")
        private String untagged;

        @JsonProperty("Deleted")
        private String deleted;
    }
}
