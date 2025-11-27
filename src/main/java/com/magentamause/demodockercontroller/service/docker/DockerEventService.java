package com.magentamause.demodockercontroller.service.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventType;
import com.github.dockerjava.core.command.EventsResultCallback;
import com.magentamause.demodockercontroller.domain.ContainerInstance;
import com.magentamause.demodockercontroller.model.ContainerStatus;
import com.magentamause.demodockercontroller.repository.ContainerInstanceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DockerEventService {

    private static final Logger log = LoggerFactory.getLogger(DockerEventService.class);

    private final DockerClient dockerClient;
    private final ContainerInstanceRepository containerInstanceRepository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public DockerEventService(@Qualifier("dockerEventsClient") DockerClient dockerClient,
                              ContainerInstanceRepository containerInstanceRepository) {
        this.dockerClient = dockerClient;
        this.containerInstanceRepository = containerInstanceRepository;
    }


	@PostConstruct
	public void init() {
		executorService.submit(this::listenForDockerEvents);
	}

	private void listenForDockerEvents() {
		EventsResultCallback callback = new EventsResultCallback() {
			@Override
			public void onNext(Event event) {
				log.debug("Received Docker event: {}", event);
				if (event.getType() == EventType.CONTAINER && ("stop".equals(event.getAction()) || "die".equals(event.getAction()))) {
					handleContainerExit(event);
				}
				super.onNext(event);
			}
		};

		try {
			dockerClient.eventsCmd().exec(callback).awaitCompletion();
		} catch (InterruptedException e) {
			log.warn("Docker event listener interrupted.");
			Thread.currentThread().interrupt();
		} catch (Exception e) {
			log.error("Error in Docker event listener. Restarting listener...", e);
			// Consider a back-off strategy before restarting
			try {
				Thread.sleep(5000); // Wait 5 seconds before retrying
				listenForDockerEvents();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
	}

	@Transactional
	protected void handleContainerExit(Event event) {
		if (event.getActor() == null || event.getActor().getId() == null) {
			return;
		}
		String dockerContainerId = event.getActor().getId();
		log.info("Handling container exit event for Docker container ID: {}", dockerContainerId);

		List<ContainerInstance> instances = containerInstanceRepository.findByDockerContainerId(dockerContainerId);
		if (!instances.isEmpty()) {
			for (ContainerInstance instance : instances) {
				if (instance.getStatus() == ContainerStatus.RUNNING) {
					// Update status based on event type
					String action = event.getAction();
					if ("die".equals(action)) {
						// 'die' event can indicate a crash or an exit with a non-zero status
						// Let's check the exit code if available
						String exitCode = event.getActor().getAttributes().get("exitCode");
						if (exitCode != null && !"0".equals(exitCode)) {
							instance.setStatus(ContainerStatus.FAILED);
							log.warn("Container {} exited with non-zero exit code {}. Marking as FAILED.", dockerContainerId, exitCode);
						} else {
							instance.setStatus(ContainerStatus.STOPPED);
							log.info("Container {} exited with exit code 0. Marking as STOPPED.", dockerContainerId);
						}
					} else if ("stop".equals(action)) {
						instance.setStatus(ContainerStatus.STOPPED);
						log.info("Container {} was stopped. Marking as STOPPED.", dockerContainerId);
					}
					containerInstanceRepository.save(instance);
				}
			}
		} else {
			log.debug("Received exit event for a Docker container not managed by this application: {}", dockerContainerId);
		}
	}

	@PreDestroy
	public void shutdown() {
		log.info("Shutting down Docker event listener.");
		executorService.shutdownNow();
	}
}