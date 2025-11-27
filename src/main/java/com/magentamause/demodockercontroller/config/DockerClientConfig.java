package com.magentamause.demodockercontroller.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration
public class DockerClientConfig {

    @Value("${docker.host:unix:///var/run/docker.sock}")
    private String dockerHost;

    @Bean
    @Primary
    @Qualifier("dockerClient")
    public DockerClient dockerClient() {
        DefaultDockerClientConfig config = buildDefaultConfig();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ofSeconds(30)) // Standard timeout for regular commands
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    @Bean
    @Qualifier("dockerEventsClient")
    public DockerClient dockerEventsClient() {
        DefaultDockerClientConfig config = buildDefaultConfig();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(10))
                .responseTimeout(Duration.ZERO) // Infinite timeout for event stream
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    private DefaultDockerClientConfig buildDefaultConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();
    }
}