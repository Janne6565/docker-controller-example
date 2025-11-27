package com.magentamause.demodockercontroller.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class VolumeMount {
    private String hostPath;
    private String containerPath;
    private boolean readOnly;
}
