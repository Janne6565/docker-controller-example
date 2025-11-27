package com.magentamause.demodockercontroller.controller;

import com.magentamause.demodockercontroller.domain.ContainerConfiguration;
import com.magentamause.demodockercontroller.domain.ContainerInstance;
import com.magentamause.demodockercontroller.dto.ContainerConfigurationRequest;
import com.magentamause.demodockercontroller.dto.ContainerConfigurationResponse;
import com.magentamause.demodockercontroller.dto.ContainerInstanceResponse;
import com.magentamause.demodockercontroller.mapper.ContainerConfigurationMapper;
import com.magentamause.demodockercontroller.mapper.ContainerInstanceMapper;
import com.magentamause.demodockercontroller.service.ContainerConfigurationService;
import com.magentamause.demodockercontroller.service.ContainerLifecycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerConfigurationService configService;
    private final ContainerLifecycleService lifecycleService;
    private final ContainerConfigurationMapper configMapper;
    private final ContainerInstanceMapper instanceMapper;

    // TODO: Add Spring Security for authentication and authorization for all endpoints

    // --- Container Configuration Endpoints ---

    @PostMapping("/configurations")
    public ResponseEntity<ContainerConfigurationResponse> createConfiguration(@Valid @RequestBody ContainerConfigurationRequest request) {
        ContainerConfiguration config = configMapper.toEntity(request);
        ContainerConfiguration savedConfig = configService.saveConfiguration(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(configMapper.toResponse(savedConfig));
    }

    @GetMapping("/configurations")
    public ResponseEntity<List<ContainerConfigurationResponse>> getAllConfigurations() {
        List<ContainerConfiguration> configurations = configService.getAllConfigurations();
        return ResponseEntity.ok(configurations.stream()
                .map(configMapper::toResponse)
                .collect(Collectors.toList()));
    }

    @DeleteMapping("/configurations/{id}")
    public ResponseEntity<Void> deleteConfiguration(@PathVariable UUID id) {
        try {
            configService.deleteConfiguration(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration not found or other error", e);
        }
    }

    // --- Container Lifecycle Endpoints ---

    @PostMapping("/containers/{configId}/create")
    public ResponseEntity<ContainerInstanceResponse> createContainerInstance(@PathVariable UUID configId) {
        ContainerConfiguration config = configService.getConfiguration(configId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuration not found"));
        ContainerInstance instance = lifecycleService.createContainer(configId, config);
        return ResponseEntity.status(HttpStatus.CREATED).body(instanceMapper.toResponse(instance));
    }

    @PostMapping("/containers/{containerId}/start")
    public ResponseEntity<ContainerInstanceResponse> startContainer(@PathVariable UUID containerId) {
        ContainerInstance instance = lifecycleService.startContainer(containerId);
        return ResponseEntity.ok(instanceMapper.toResponse(instance));
    }

    @PostMapping("/containers/{containerId}/stop")
    public ResponseEntity<ContainerInstanceResponse> stopContainer(@PathVariable UUID containerId) {
        ContainerInstance instance = lifecycleService.stopContainer(containerId);
        return ResponseEntity.ok(instanceMapper.toResponse(instance));
    }

    @DeleteMapping("/containers/{containerId}")
    public ResponseEntity<Void> deleteContainer(@PathVariable UUID containerId) {
        try {
            lifecycleService.deleteContainer(containerId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete container", e);
        }
    }

    @GetMapping("/containers")
    public ResponseEntity<List<ContainerInstanceResponse>> getAllContainerInstances() {
        List<ContainerInstance> instances = lifecycleService.getAllContainerInstances();
        return ResponseEntity.ok(instanceMapper.toResponseList(instances));
    }

    @GetMapping("/containers/{containerId}")
    public ResponseEntity<ContainerInstanceResponse> getContainerInstance(@PathVariable UUID containerId) {
        ContainerInstance instance = lifecycleService.getContainerInstance(containerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Container instance not found"));
        return ResponseEntity.ok(instanceMapper.toResponse(instance));
    }
}