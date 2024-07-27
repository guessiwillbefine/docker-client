package ua.storozhuk.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class HostConfig {

    @JsonProperty("NetworkMode")
    private String networkMode;

    @JsonProperty("PortBindings")
    private Map<String, List<PortBindingsPair.PortPair>> portBindings;

    @JsonProperty("NetworkingConfig")
    private NetworkConfig networkingConfig;

    @Getter
    @AllArgsConstructor
    public static class PortBindingsPair {

        @Getter
        @AllArgsConstructor
        public static class PortPair {

            @JsonProperty("HostIp")
            private String hostIP;

            @JsonProperty("HostPort")
            private String hostPort;
        }
    }
}
