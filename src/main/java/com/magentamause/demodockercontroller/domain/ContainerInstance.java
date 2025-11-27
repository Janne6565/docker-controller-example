package com.magentamause.demodockercontroller.domain;

import com.magentamause.demodockercontroller.model.ContainerStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
public class ContainerInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID configurationId;
    private String dockerContainerId;

    @Enumerated(EnumType.STRING)
    private ContainerStatus status;

    private Instant createdAt;
    private Instant startedAt;
}
