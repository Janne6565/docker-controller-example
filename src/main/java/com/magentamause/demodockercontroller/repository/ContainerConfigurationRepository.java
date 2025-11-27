package com.magentamause.demodockercontroller.repository;

import com.magentamause.demodockercontroller.domain.ContainerConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContainerConfigurationRepository extends JpaRepository<ContainerConfiguration, UUID> {
}
