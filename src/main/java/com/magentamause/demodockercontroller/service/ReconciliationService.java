package com.magentamause.demodockercontroller.service;

import com.github.dockerjava.api.model.Container;
import com.magentamause.demodockercontroller.domain.ContainerInstance;
import com.magentamause.demodockercontroller.model.ContainerStatus;
import com.magentamause.demodockercontroller.repository.ContainerInstanceRepository;
import com.magentamause.demodockercontroller.service.docker.DockerService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final ContainerInstanceRepository containerInstanceRepository;
    private final DockerService dockerService;
    private final ContainerLifecycleService containerLifecycleService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconcileContainersOnStartup() {
        log.info("Starting container reconciliation on application startup.");

        List<ContainerInstance> dbInstances = containerInstanceRepository.findAll();
        List<Container> dockerContainers = dockerService.listAllContainers();

        Map<String, Container> dockerContainerMap = dockerContainers.stream()
                .collect(Collectors.toMap(Container::getId, Function.identity()));

        for (ContainerInstance dbInstance : dbInstances) {
            String dockerContainerId = dbInstance.getDockerContainerId();
            if (dockerContainerId == null) {
                log.warn("Database instance {} has no Docker Container ID. Setting status to FAILED.", dbInstance.getId());
                dbInstance.setStatus(ContainerStatus.FAILED);
                containerInstanceRepository.save(dbInstance);
                continue;
            }

            if (dockerContainerMap.containsKey(dockerContainerId)) {
                // Container exists in Docker, update status based on Docker's state
                Container dockerContainer = dockerContainerMap.get(dockerContainerId);
                ContainerStatus currentDockerStatus = getDockerStatus(dockerContainer);

                if (dbInstance.getStatus() != currentDockerStatus) {
                    log.info("Reconciling instance {}: DB status {} -> Docker status {}",
                            dbInstance.getId(), dbInstance.getStatus(), currentDockerStatus);
                    dbInstance.setStatus(currentDockerStatus);
                    containerInstanceRepository.save(dbInstance);
                }
            } else {
                // Container does not exist in Docker, mark as FAILED in DB if it was not already
                if (dbInstance.getStatus() != ContainerStatus.FAILED && dbInstance.getStatus() != ContainerStatus.STOPPED) {
                    log.warn("Docker container {} for instance {} not found. Setting DB status to FAILED.",
                            dockerContainerId, dbInstance.getId());
                    dbInstance.setStatus(ContainerStatus.FAILED);
                    containerInstanceRepository.save(dbInstance);
                }
            }
        }
        log.info("Container reconciliation completed.");
    }

    private ContainerStatus getDockerStatus(Container dockerContainer) {
        // Docker-java container status strings can be varied (e.g., "running", "exited", "created")
        String status = dockerContainer.getStatus() != null ? dockerContainer.getStatus().toLowerCase() : "";
        if (status.contains("running")) {
            return ContainerStatus.RUNNING;
        } else if (status.contains("exited")) {
            return ContainerStatus.STOPPED;
        } else if (status.contains("created")) {
            return ContainerStatus.CREATED;
        }
        // Fallback or more detailed inspection if needed
        return containerLifecycleService.getDockerContainerStatus(dockerContainer.getId());
    }
}
