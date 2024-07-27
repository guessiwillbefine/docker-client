package ua.storozhuk.client;

import ua.storozhuk.dto.container.request.DockerContainerCreationDto;
import ua.storozhuk.dto.container.response.DockerContainerCreationResponse;
import ua.storozhuk.dto.container.response.DockerContainerDto;
import ua.storozhuk.dto.image.response.DockerImageDto;
import ua.storozhuk.dto.image.response.DockerPrunedDto;

import java.util.List;

/**
 * Docker API client
 */
public interface DockerClient {

    /** Returns list of all docker images */
    List<DockerImageDto> getAllImages();

    /** Returns list of all docker containers */
    List<DockerContainerDto> getAllContainers();

    /** Creates docker container with specified container name, without starting */
    DockerContainerCreationResponse createContainer(String containerName, DockerContainerCreationDto creationDto);

    /** Deletes all images from docker */
    DockerPrunedDto pruneImages();

    /**
     *
     * @param pathToProject - absolute path where project stores. must be .tar.gz archive
     * @param containerName - name of built image. Optional, null if don't need to set up custom name
     */
    String buildImage(String pathToProject, String containerName);

    /** Starts container by its id */
    void startContainer(String id);

    /** Stops container by its id */
    boolean stopContainer(String id);

    //TODO add filtering by labels (https://docs.docker.com/engine/api/v1.45/#tag/Container/operation/ContainerPrune)
    /** Delete all stopped containers */
    void deleteStoppedContainers();
}
