package com.magentamause.demodockercontroller.dto;

import com.magentamause.demodockercontroller.model.ContainerStatus;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class ContainerInstanceResponse {
    private UUID id;
    private UUID configurationId;
    private String dockerContainerId;
    private ContainerStatus status;
    private Instant createdAt;
    private Instant startedAt;
}
