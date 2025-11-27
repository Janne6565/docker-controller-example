# Docker Utility Backend (Spring Boot)

This is a production-ready Java Spring Boot backend application designed to manage Docker containers via a REST API. It utilizes a clean layered architecture, JPA for persistence, and `docker-java` for Docker daemon communication.

## Table of Contents
- [Architecture](#architecture)
- [Technologies](#technologies)
- [Core Domain Model](#core-domain-model)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Building the Project](#building-the-project)
  - [Running the Application](#running-the-application)
  - [Docker Socket Mounting](#docker-socket-mounting)
- [API Endpoints](#api-endpoints)
  - [Container Configuration](#container-configuration)
  - [Container Lifecycle](#container-lifecycle)
- [Error Handling](#error-handling)
- [Security Notes](#security-notes)
- [Future Enhancements (TODOs)](#future-enhancements-todos)

## Architecture

The application follows a clean layered architecture:

-   **Controller Layer**: Exposes REST endpoints for client interaction.
-   **Service Layer**: Contains the business logic for managing container configurations and lifecycle.
-   **Persistence Layer**: Handles data storage and retrieval using JPA/Hibernate with an H2 in-memory database (configurable for PostgreSQL).
-   **Docker Integration Layer**: Communicates with the Docker daemon using the `docker-java` library, abstracting Docker API calls.

DTOs (Data Transfer Objects) are used for all request and response payloads to prevent direct exposure of entity models.

## Technologies

-   **Java 17+**
-   **Spring Boot 3+**
-   **Spring Data JPA**
-   **H2 Database** (in-memory for development, easily configurable for PostgreSQL)
-   **Lombok**
-   **docker-java** (version 3.3.6)
-   **Maven**

## Core Domain Model

### `ContainerConfiguration` (Persistent)
Represents the blueprint for a Docker container.
-   `id` (UUID)
-   `imageName` (String)
-   `imageTag` (String)
-   `command` (List<String>)
-   `portMappings` (Map<Integer, Integer>): Host port → Container port
-   `envVariables` (Map<String, String>)
-   `volumeMounts` (List<VolumeMount>)
-   `resourceLimits` (ResourceLimits)

### `VolumeMount` (Embedded)
Defines how a host path is mounted into a container.
-   `hostPath` (String)
-   `containerPath` (String)
-   `readOnly` (boolean)

### `ResourceLimits` (Embedded)
Specifies CPU and memory constraints for a container.
-   `memoryBytes` (Long)
-   `cpuCores` (Double)

### `ContainerInstance` (Runtime State)
Represents an actual running or stopped Docker container.
-   `id` (UUID)
-   `configurationId` (UUID): Links to the `ContainerConfiguration`
-   `dockerContainerId` (String): The ID assigned by the Docker daemon
-   `status` (Enum: CREATED, RUNNING, STOPPED, FAILED)
-   `createdAt` (Instant)
-   `startedAt` (Instant)

## Getting Started

### Prerequisites

-   Java Development Kit (JDK) 17 or higher
-   Apache Maven 3.6.0 or higher
-   Docker daemon running on your host machine.
-   **Crucially**: The application needs access to the Docker daemon via its Unix socket (`/var/run/docker.sock`). Ensure your user has permissions to access this socket (e.g., by being part of the `docker` group).

### Building the Project

Navigate to the project root directory and run:
```bash
mvn clean install
```

### Running the Application

You can run the Spring Boot application using Maven:
```bash
mvn spring-boot:run
```
Or, after building, run the JAR file:
```bash
java -jar target/demoDockerController-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`.

### Docker Socket Mounting

For the application to communicate with your Docker daemon, it needs access to the Docker Unix socket. When running the application as a standalone JAR, this access is typically inherited from the user running the application.

**Important:** Ensure the user running this Spring Boot application has read/write permissions to `/var/run/docker.sock`. On Linux, this usually means adding the user to the `docker` group:
```bash
sudo usermod -aG docker $USER
newgrp docker # Apply group changes, or log out/in
```

If you were to containerize *this* application, you would mount the Docker socket into its container:
```bash
docker run -v /var/run/docker.sock:/var/run/docker.sock -p 8080:8080 your-image-name
```
(This application is designed to be run directly on a host that has Docker installed, not necessarily *inside* a Docker container itself, unless explicitly configured for Docker-in-Docker or similar patterns).

## API Endpoints

All API endpoints are prefixed with `/`.

### Container Configuration

| Method | Path                        | Description                                     | Request Body              | Response Body                      |
| :----- | :-------------------------- | :---------------------------------------------- | :------------------------ | :--------------------------------- |
| `POST` | `/configurations`           | Creates a new container configuration.          | `ContainerConfigurationRequest` | `ContainerConfigurationResponse`   |
| `GET`  | `/configurations`           | Retrieves all container configurations.         | None                      | List of `ContainerConfigurationResponse` |
| `DELETE` | `/configurations/{id}`      | Deletes a container configuration by ID. Fails if active containers exist. | None                      | `204 No Content`                   |

### Container Lifecycle

| Method | Path                               | Description                                     | Request Body | Response Body                      |
| :----- | :--------------------------------- | :---------------------------------------------- | :----------- | :--------------------------------- |
| `POST` | `/containers/{configId}/create`    | Creates a new Docker container instance from a configuration. | None         | `ContainerInstanceResponse`        |
| `POST` | `/containers/{containerId}/start`  | Starts an existing Docker container instance.   | None         | `ContainerInstanceResponse`        |
| `POST` | `/containers/{containerId}/stop`   | Stops an existing Docker container instance.    | None         | `ContainerInstanceResponse`        |
| `GET`  | `/containers`                      | Retrieves all container instances.              | None         | List of `ContainerInstanceResponse` |
| `GET`  | `/containers/{containerId}`        | Retrieves a specific container instance by ID.  | None         | `ContainerInstanceResponse`        |

## Error Handling

The application provides consistent error responses using `@ControllerAdvice`.
-   `404 Not Found`: For resources that do not exist (e.g., configuration or instance IDs).
-   `409 Conflict`: When an operation cannot be completed due to a conflict (e.g., trying to delete a configuration with active containers).
-   `400 Bad Request`: For validation errors in request bodies.
-   `500 Internal Server Error`: For unexpected server-side issues, including Docker daemon errors.

## Security Notes

-   **Docker Socket Access**: Granting access to the Docker socket is equivalent to granting root access on the host. This application assumes a trusted environment.
-   **Authentication/Authorization**: Currently, no authentication or authorization is implemented.
    -   **TODO**: Integrate Spring Security for robust authentication and role-based authorization to secure the API endpoints.

## Future Enhancements (TODOs)

-   Implement robust authentication and authorization using Spring Security.
-   Add more detailed logging for Docker operations, potentially with progress streaming for image pulls.
-   Introduce a health endpoint to specifically check Docker daemon connectivity.
-   Expand resource limits to include CPU shares, CPU period, and CPU quota for more granular control.
-   Implement graceful shutdown of containers when the application stops.
-   Add support for PostgreSQL database.
