package com.magentamause.demodockercontroller.service;

import com.magentamause.demodockercontroller.domain.ContainerConfiguration;
import com.magentamause.demodockercontroller.repository.ContainerConfigurationRepository;
import com.magentamause.demodockercontroller.repository.ContainerInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContainerConfigurationService {

    private final ContainerConfigurationRepository configRepository;
    private final ContainerInstanceRepository instanceRepository;

    @Transactional
    public ContainerConfiguration saveConfiguration(ContainerConfiguration configuration) {
        return configRepository.save(configuration);
    }

    @Transactional(readOnly = true)
    public Optional<ContainerConfiguration> getConfiguration(UUID id) {
        return configRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ContainerConfiguration> getAllConfigurations() {
        return configRepository.findAll();
    }

    @Transactional
    public void deleteConfiguration(UUID id) {
        // Check if there are active containers for this configuration
        if (!instanceRepository.findByConfigurationId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete configuration with active container instances.");
        }
        configRepository.deleteById(id);
    }
}
