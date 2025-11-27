package com.magentamause.demodockercontroller.mapper;

import com.magentamause.demodockercontroller.domain.ContainerInstance;
import com.magentamause.demodockercontroller.dto.ContainerInstanceResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ContainerInstanceMapper {

    public ContainerInstanceResponse toResponse(ContainerInstance entity) {
        if (entity == null) {
            return null;
        }
        ContainerInstanceResponse dto = new ContainerInstanceResponse();
        dto.setId(entity.getId());
        dto.setConfigurationId(entity.getConfigurationId());
        dto.setDockerContainerId(entity.getDockerContainerId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setStartedAt(entity.getStartedAt());
        return dto;
    }

    public List<ContainerInstanceResponse> toResponseList(List<ContainerInstance> entities) {
        return Optional.ofNullable(entities)
                .map(list -> list.stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList()))
                .orElse(null);
    }
}
