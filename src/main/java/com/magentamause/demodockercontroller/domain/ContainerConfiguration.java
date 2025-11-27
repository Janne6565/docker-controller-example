package com.magentamause.demodockercontroller.domain;

import com.magentamause.demodockercontroller.model.ResourceLimits;
import com.magentamause.demodockercontroller.model.VolumeMount;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Entity
public class ContainerConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String imageName;
    private String imageTag;

    @ElementCollection
    private List<String> command;

    @ElementCollection
    @CollectionTable(name = "container_port_mappings",
            joinColumns = @JoinColumn(name = "configuration_id"))
    @MapKeyColumn(name = "host_port")
    @Column(name = "container_port")
    private Map<Integer, Integer> portMappings;

    @ElementCollection
    @CollectionTable(name = "container_env_variables",
            joinColumns = @JoinColumn(name = "configuration_id"))
    @MapKeyColumn(name = "env_key")
    @Column(name = "env_value")
    private Map<String, String> envVariables;

    @ElementCollection
    @CollectionTable(name = "container_volume_mounts",
            joinColumns = @JoinColumn(name = "configuration_id"))
    private List<VolumeMount> volumeMounts;

    @Embedded
    private ResourceLimits resourceLimits;
}
