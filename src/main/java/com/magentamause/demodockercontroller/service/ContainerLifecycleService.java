package com.magentamause.demodockercontroller.service;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.magentamause.demodockercontroller.domain.ContainerConfiguration;
import com.magentamause.demodockercontroller.domain.ContainerInstance;
import com.magentamause.demodockercontroller.model.ContainerStatus;
import com.magentamause.demodockercontroller.repository.ContainerInstanceRepository;
import com.magentamause.demodockercontroller.service.docker.DockerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContainerLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(ContainerLifecycleService.class);

    private final DockerService dockerService;
    private final ContainerInstanceRepository containerInstanceRepository;

    @Transactional
    public ContainerInstance createContainer(UUID configurationId, ContainerConfiguration config) {
        // TODO: Validate if config exists
        // TODO: Error handling for Docker operations

        String dockerContainerId = dockerService.createContainer(
                config.getImageName(),
                config.getImageTag(),
                config.getCommand(),
                config.getPortMappings(),
                config.getEnvVariables(),
                config.getVolumeMounts(),
                config.getResourceLimits()
        );

        ContainerInstance instance = new ContainerInstance();
        instance.setConfigurationId(configurationId);
        instance.setDockerContainerId(dockerContainerId);
        instance.setStatus(ContainerStatus.CREATED);
        instance.setCreatedAt(Instant.now());

        return containerInstanceRepository.save(instance);
    }

    @Transactional
    public ContainerInstance startContainer(UUID instanceId) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container instance not found"));

        if (instance.getDockerContainerId() == null) {
            throw new IllegalStateException("Docker container ID is missing for instance: " + instanceId);
        }

        dockerService.startContainer(instance.getDockerContainerId());
        instance.setStatus(ContainerStatus.RUNNING);
        instance.setStartedAt(Instant.now());
        return containerInstanceRepository.save(instance);
    }

    @Transactional
    public ContainerInstance stopContainer(UUID instanceId) {
        ContainerInstance instance = containerInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Container instance not found"));

        if (instance.getDockerContainerId() == null) {
            throw new IllegalStateException("Docker container ID is missing for instance: " + instanceId);
        }

        dockerService.stopContainer(instance.getDockerContainerId());
        instance.setStatus(ContainerStatus.STOPPED);
        return containerInstanceRepository.save(instance);
    }

    @Transactional(readOnly = true)
    public Optional<ContainerInstance> getContainerInstance(UUID instanceId) {
        return containerInstanceRepository.findById(instanceId);
    }

    @Transactional(readOnly = true)
    public List<ContainerInstance> getAllContainerInstances() {
        return containerInstanceRepository.findAll();
    }

    // This method will be used by reconciliation and potentially by inspect endpoint
    public ContainerStatus getDockerContainerStatus(String dockerContainerId) {
        InspectContainerResponse inspectResponse = dockerService.inspectContainer(dockerContainerId);
        if (inspectResponse == null || inspectResponse.getState() == null) {
            return ContainerStatus.FAILED; // Or some other appropriate status
        }

        if (Boolean.TRUE.equals(inspectResponse.getState().getRunning())) {
            return ContainerStatus.RUNNING;
        } else if (Boolean.TRUE.equals(inspectResponse.getState().getPaused())) {
            return ContainerStatus.STOPPED; // Paused containers are not considered RUNNING
        } else if (Boolean.TRUE.equals(inspectResponse.getState().getDead())) {
            return ContainerStatus.FAILED; // Dead containers are failed
        } else if (Boolean.TRUE.equals(inspectResponse.getState().getOOMKilled())) {
            return ContainerStatus.FAILED; // OOMKilled containers are failed
        }
        return ContainerStatus.CREATED; // Default or unknown state
    }
}