package com.magentamause.demodockercontroller.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class ResourceLimits {
    private Long memoryBytes;
    private Double cpuCores;
}
