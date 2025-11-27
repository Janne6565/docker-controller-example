package com.magentamause.demodockercontroller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ContainerConfigurationRequest {
    @NotBlank
    private String imageName;
    @NotBlank
    private String imageTag;

    private List<String> command;

    private Map<Integer, Integer> portMappings;

    private Map<String, String> envVariables;

    private List<@Valid VolumeMountDto> volumeMounts;

    @Valid
    @NotNull
    private ResourceLimitsDto resourceLimits;
}
