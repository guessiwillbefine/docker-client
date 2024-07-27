package ua.storozhuk.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "docker.client")
public class DockerClientConfigurationSource {

    private String version;

    private String host;
}
