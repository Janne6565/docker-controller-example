package com.magentamause.demodockercontroller.mapper;

import com.magentamause.demodockercontroller.dto.ContainerConfigurationRequest;
import com.magentamause.demodockercontroller.dto.ContainerConfigurationResponse;
import com.magentamause.demodockercontroller.dto.ResourceLimitsDto;
import com.magentamause.demodockercontroller.dto.VolumeMountDto;
import com.magentamause.demodockercontroller.domain.ContainerConfiguration;
import com.magentamause.demodockercontroller.model.ResourceLimits;
import com.magentamause.demodockercontroller.model.VolumeMount;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ContainerConfigurationMapper {

    public ContainerConfiguration toEntity(ContainerConfigurationRequest request) {
        if (request == null) {
            return null;
        }
        ContainerConfiguration config = new ContainerConfiguration();
        config.setImageName(request.getImageName());
        config.setImageTag(request.getImageTag());
        config.setCommand(request.getCommand());
        config.setPortMappings(request.getPortMappings());
        config.setEnvVariables(request.getEnvVariables());
        config.setVolumeMounts(toVolumeMountList(request.getVolumeMounts()));
        config.setResourceLimits(toResourceLimits(request.getResourceLimits()));
        return config;
    }

    public ContainerConfigurationResponse toResponse(ContainerConfiguration config) {
        if (config == null) {
            return null;
        }
        ContainerConfigurationResponse response = new ContainerConfigurationResponse();
        response.setId(config.getId());
        response.setImageName(config.getImageName());
        response.setImageTag(config.getImageTag());
        response.setCommand(config.getCommand());
        response.setPortMappings(config.getPortMappings());
        response.setEnvVariables(config.getEnvVariables());
        response.setVolumeMounts(toVolumeMountDtoList(config.getVolumeMounts()));
        response.setResourceLimits(toResourceLimitsDto(config.getResourceLimits()));
        return response;
    }

    public void updateEntityFromRequest(ContainerConfigurationRequest request, ContainerConfiguration config) {
        if (request == null || config == null) {
            return;
        }
        config.setImageName(request.getImageName());
        config.setImageTag(request.getImageTag());
        config.setCommand(request.getCommand());
        config.setPortMappings(request.getPortMappings());
        config.setEnvVariables(request.getEnvVariables());
        config.setVolumeMounts(toVolumeMountList(request.getVolumeMounts()));
        config.setResourceLimits(toResourceLimits(request.getResourceLimits()));
    }

    private VolumeMount toVolumeMount(VolumeMountDto dto) {
        if (dto == null) {
            return null;
        }
        VolumeMount volumeMount = new VolumeMount();
        volumeMount.setHostPath(dto.getHostPath());
        volumeMount.setContainerPath(dto.getContainerPath());
        volumeMount.setReadOnly(dto.getReadOnly());
        return volumeMount;
    }

    private VolumeMountDto toVolumeMountDto(VolumeMount entity) {
        if (entity == null) {
            return null;
        }
        VolumeMountDto dto = new VolumeMountDto();
        dto.setHostPath(entity.getHostPath());
        dto.setContainerPath(entity.getContainerPath());
        dto.setReadOnly(entity.isReadOnly());
        return dto;
    }

    private List<VolumeMount> toVolumeMountList(List<VolumeMountDto> dtoList) {
        return Optional.ofNullable(dtoList)
                .map(list -> list.stream()
                        .map(this::toVolumeMount)
                        .collect(Collectors.toList()))
                .orElse(null);
    }

    private List<VolumeMountDto> toVolumeMountDtoList(List<VolumeMount> entityList) {
        return Optional.ofNullable(entityList)
                .map(list -> list.stream()
                        .map(this::toVolumeMountDto)
                        .collect(Collectors.toList()))
                .orElse(null);
    }

    private ResourceLimits toResourceLimits(ResourceLimitsDto dto) {
        if (dto == null) {
            return null;
        }
        ResourceLimits resourceLimits = new ResourceLimits();
        resourceLimits.setMemoryBytes(dto.getMemoryBytes());
        resourceLimits.setCpuCores(dto.getCpuCores());
        return resourceLimits;
    }

    private ResourceLimitsDto toResourceLimitsDto(ResourceLimits entity) {
        if (entity == null) {
            return null;
        }
        ResourceLimitsDto dto = new ResourceLimitsDto();
        dto.setMemoryBytes(entity.getMemoryBytes());
        dto.setCpuCores(entity.getCpuCores());
        return dto;
    }
}
