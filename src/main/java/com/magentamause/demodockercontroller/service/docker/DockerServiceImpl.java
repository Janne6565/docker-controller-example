package com.magentamause.demodockercontroller.service.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.magentamause.demodockercontroller.model.ResourceLimits;
import com.magentamause.demodockercontroller.model.VolumeMount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DockerServiceImpl implements DockerService {

    private static final Logger log = LoggerFactory.getLogger(DockerServiceImpl.class);

    private final DockerClient dockerClient;

    public DockerServiceImpl(@Qualifier("dockerClient") DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void pullImage(String imageName, String imageTag) {
        String fullImageName = imageName + ":" + imageTag;
        log.info("Attempting to pull image: {}", fullImageName);
        try {
            dockerClient.pullImageCmd(fullImageName)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion();
            log.info("Successfully pulled image: {}", fullImageName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Image pull interrupted for {}: {}", fullImageName, e.getMessage());
            throw new RuntimeException("Image pull interrupted", e);
        } catch (NotFoundException e) {
            log.error("Image {} not found: {}", fullImageName, e.getMessage());
            throw new RuntimeException("Image not found", e);
        } catch (Exception e) {
            log.error("Failed to pull image {}: {}", fullImageName, e.getMessage());
            throw new RuntimeException("Failed to pull image", e);
        }
    }

    @Override
    public String createContainer(String imageName, String imageTag, List<String> command,
                                  Map<Integer, Integer> portMappings, Map<String, String> envVariables,
                                  List<VolumeMount> volumeMounts, ResourceLimits resourceLimits) {
        String fullImageName = imageName + ":" + imageTag;
        log.info("Creating container from image: {}", fullImageName);

        // Pull image if not present (or handle error if pull fails)
        try {
            dockerClient.inspectImageCmd(fullImageName).exec();
        } catch (NotFoundException e) {
            log.warn("Image {} not found locally, pulling...", fullImageName);
            pullImage(imageName, imageTag);
        }

        HostConfig hostConfig = HostConfig.newHostConfig();

        // Port mappings
        ExposedPort[] exposedPorts = Optional.ofNullable(portMappings).orElse(Collections.emptyMap()).keySet().stream()
                .map(ExposedPort::tcp)
                .toArray(ExposedPort[]::new);

        if (portMappings != null && !portMappings.isEmpty()) {
            Ports ports = new Ports();
            portMappings.forEach((hostPort, containerPort) ->
                    ports.bind(ExposedPort.tcp(containerPort), Ports.Binding.bindPort(hostPort)));
            hostConfig.withPortBindings(ports);
        }

        // Volume mounts
        List<Bind> binds = Optional.ofNullable(volumeMounts).orElse(Collections.emptyList()).stream()
                .filter(Objects::nonNull)
                .map(vm -> new Bind(vm.getHostPath(), new Volume(vm.getContainerPath()), vm.isReadOnly() ? AccessMode.ro : AccessMode.rw))
                .collect(Collectors.toList());
        if (!binds.isEmpty()) {
            hostConfig.withBinds(binds);
        }

        // Resource limits
        if (resourceLimits != null) {
            if (resourceLimits.getMemoryBytes() != null) {
                hostConfig.withMemory(resourceLimits.getMemoryBytes());
            }
            if (resourceLimits.getCpuCores() != null) {
                // Docker expects CPU period and quota for CPU limits
                // For simplicity, let's use a common conversion or consider `NanoCpus`
                // Here, we'll use a simple approximation: 1 core = 1024 shares, or directly NanoCpus
                // For fine-grained control, period/quota are better.
                // Let's use NanoCpus for simplicity as it maps directly to cores.
                // A value of 1_000_000_000 (10^9) means 1 CPU core.
                hostConfig.withNanoCPUs((long) (resourceLimits.getCpuCores() * 1_000_000_000));
            }
        }

        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(fullImageName)
                .withHostConfig(hostConfig)
                .withExposedPorts(exposedPorts)
                .withAttachStderr(true)
                .withAttachStdout(true);

        // Command
        if (command != null && !command.isEmpty()) {
            createContainerCmd.withCmd(command);
        }

        // Environment variables
        if (envVariables != null && !envVariables.isEmpty()) {
            List<String> env = envVariables.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.toList());
            createContainerCmd.withEnv(env);
        }

        try {
            CreateContainerResponse containerResponse = createContainerCmd.exec();
            log.info("Container created with ID: {}", containerResponse.getId());
            return containerResponse.getId();
        } catch (Exception e) {
            log.error("Failed to create container from image {}: {}", fullImageName, e.getMessage());
            throw new RuntimeException("Failed to create container", e);
        }
    }

    @Override
    public void startContainer(String containerId) {
        log.info("Starting container with ID: {}", containerId);
        try {
            dockerClient.startContainerCmd(containerId).exec();
            log.info("Container {} started.", containerId);
        } catch (NotFoundException e) {
            log.error("Container {} not found for starting: {}", containerId, e.getMessage());
            throw new RuntimeException("Container not found", e);
        } catch (Exception e) {
            log.error("Failed to start container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to start container", e);
        }
    }

    @Override
    public void stopContainer(String containerId) {
        log.info("Stopping container with ID: {}", containerId);
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            log.info("Container {} stopped.", containerId);
        } catch (NotFoundException e) {
            log.error("Container {} not found for stopping: {}", containerId, e.getMessage());
            throw new RuntimeException("Container not found", e);
        } catch (Exception e) {
            log.error("Failed to stop container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to stop container", e);
        }
    }

    @Override
    public void deleteContainer(String containerId) {
        log.info("Deleting container with ID: {}", containerId);
        try {
            // First, stop the container. If it's already stopped, this will do nothing.
            // A NotFoundException here means it's already gone, which is fine.
            try {
                dockerClient.stopContainerCmd(containerId).exec();
                log.info("Container {} stopped before deletion.", containerId);
            } catch (NotFoundException e) {
                log.warn("Container {} not found for stopping before deletion. It may have already been removed.", containerId);
                // If the container doesn't exist, we don't need to do anything else.
                return;
            } catch (NotModifiedException e) {
                log.info("Container {} was already stopped.", containerId);
                // If container is already stopped, we can proceed to delete it.
            }

            // Now, remove the container
            dockerClient.removeContainerCmd(containerId).exec();
            log.info("Container {} deleted successfully.", containerId);
        } catch (NotFoundException e) {
            log.warn("Container {} not found for deletion. It was likely already removed.", containerId);
        } catch (Exception e) {
            log.error("Failed to delete container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to delete container", e);
        }
    }

    @Override
    public InspectContainerResponse inspectContainer(String containerId) {
        log.debug("Inspecting container with ID: {}", containerId);
        try {
            return dockerClient.inspectContainerCmd(containerId).exec();
        } catch (NotFoundException e) {
            log.warn("Container {} not found during inspection: {}", containerId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Failed to inspect container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Failed to inspect container", e);
        }
    }

    @Override
    public List<Container> listAllContainers() {
        log.debug("Listing all containers.");
        try {
            return dockerClient.listContainersCmd().withShowAll(true).exec();
        } catch (Exception e) {
            log.error("Failed to list containers: {}", e.getMessage());
            throw new RuntimeException("Failed to list containers", e);
        }
    }

    @Override
    public boolean containerExists(String containerId) {
        try {
            dockerClient.inspectContainerCmd(containerId).exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking existence of container {}: {}", containerId, e.getMessage());
            throw new RuntimeException("Error checking container existence", e);
        }
    }
}