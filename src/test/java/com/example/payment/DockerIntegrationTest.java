package com.example.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
public class DockerIntegrationTest {

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("paymentdb")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.11-management");

    @Test
    void containersStart() {
        assertTrue(postgres.isRunning());
        assertTrue(rabbitmq.isRunning());
    }
}
