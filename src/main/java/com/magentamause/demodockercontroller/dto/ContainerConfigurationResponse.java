package com.magentamause.demodockercontroller.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ContainerConfigurationResponse {
    private UUID id;
    private String imageName;
    private String imageTag;
    private List<String> command;
    private Map<Integer, Integer> portMappings;
    private Map<String, String> envVariables;
    private List<VolumeMountDto> volumeMounts;
    private ResourceLimitsDto resourceLimits;
}
