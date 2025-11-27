package com.magentamause.demodockercontroller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResourceLimitsDto {
    @NotNull
    private Long memoryBytes;
    @NotNull
    private Double cpuCores;
}
