package com.magentamause.demodockercontroller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VolumeMountDto {
    @NotBlank
    private String hostPath;
    @NotBlank
    private String containerPath;
    @NotNull
    private Boolean readOnly;
}
