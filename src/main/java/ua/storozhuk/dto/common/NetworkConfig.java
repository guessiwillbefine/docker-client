package ua.storozhuk.dto.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class NetworkConfig {
    @JsonProperty("EndpointsConfig")
    private Map<String, Object> endpointsConfig;
}
