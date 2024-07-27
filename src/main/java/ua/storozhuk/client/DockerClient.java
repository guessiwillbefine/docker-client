package ua.storozhuk.client;

import ua.storozhuk.dto.container.request.DockerContainerCreationDto;
import ua.storozhuk.dto.container.response.DockerContainerCreationResponse;
import ua.storozhuk.dto.container.response.DockerContainerDto;
import ua.storozhuk.dto.image.response.DockerImageDto;
import ua.storozhuk.dto.image.response.DockerPrunedDto;

import java.util.List;

public interface DockerClient {

    List<DockerImageDto> getAllImages();

    List<DockerContainerDto> getAllContainers();

    DockerContainerCreationResponse createContainer(String containerName, DockerContainerCreationDto creationDto);

    DockerPrunedDto pruneImages();

    /**
     *
     * @param pathToProject - absolute path where project stores. must be .tar.gz archive
     * @param containerName - name of built image. Optional, null if don't need to set up custom name
     */
    String buildImage(String pathToProject, String containerName);

    void startContainer(String id);

    boolean stopContainer(String id);

    void deleteStoppedContainers();
}
