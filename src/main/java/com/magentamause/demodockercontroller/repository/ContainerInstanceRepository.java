package com.magentamause.demodockercontroller.repository;

import com.magentamause.demodockercontroller.domain.ContainerInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContainerInstanceRepository extends JpaRepository<ContainerInstance, UUID> {
    List<ContainerInstance> findByConfigurationId(UUID configurationId);
    List<ContainerInstance> findByDockerContainerId(String dockerContainerId);
}
