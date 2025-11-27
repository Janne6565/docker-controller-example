package com.magentamause.demodockercontroller.service.docker;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.magentamause.demodockercontroller.domain.ContainerConfiguration;
import com.magentamause.demodockercontroller.model.ResourceLimits;
import com.magentamause.demodockercontroller.model.VolumeMount;

import java.util.List;
import java.util.Map;

public interface DockerService {
    void pullImage(String imageName, String imageTag);
    String createContainer(String imageName, String imageTag, List<String> command,
                           Map<Integer, Integer> portMappings, Map<String, String> envVariables,
                           List<VolumeMount> volumeMounts, ResourceLimits resourceLimits);
    void startContainer(String containerId);
    void stopContainer(String containerId);
    void deleteContainer(String containerId);
    InspectContainerResponse inspectContainer(String containerId);
    List<Container> listAllContainers();
    boolean containerExists(String containerId);
}
