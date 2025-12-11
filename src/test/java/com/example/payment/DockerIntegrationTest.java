package com.example.payment;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import java.time.Duration;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class DockerIntegrationTest {

        @Container
        public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("paymentdb")
            .withUsername("postgres")
            .withPassword("postgres")
            .withStartupTimeout(Duration.ofSeconds(60));

        @Container
        public static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.11-management")
            .withStartupTimeout(Duration.ofSeconds(60));

    @Test
    @Timeout(120)
    void containersProvideConnectivity() throws Exception {
        // Postgres connectivity: create table, insert, query
        assertTrue(postgres.isRunning());
        String jdbcUrl = postgres.getJdbcUrl();
        String user = postgres.getUsername();
        String pass = postgres.getPassword();
        try (java.sql.Connection c = java.sql.DriverManager.getConnection(jdbcUrl, user, pass)) {
            try (java.sql.Statement s = c.createStatement()) {
                s.executeUpdate("CREATE TABLE IF NOT EXISTS tc_test(id SERIAL PRIMARY KEY, val TEXT);");
                s.executeUpdate("INSERT INTO tc_test(val) VALUES ('hello');");
                try (java.sql.ResultSet rs = s.executeQuery("SELECT count(*) FROM tc_test;")) {
                    if (rs.next()) {
                        int cnt = rs.getInt(1);
                        assertTrue(cnt >= 1);
                    }
                }
            }
        }

        // RabbitMQ connectivity: declare queue, publish and consume
        assertTrue(rabbitmq.isRunning());
        ConnectionFactory cf = new ConnectionFactory();
        cf.setHost(rabbitmq.getHost());
        cf.setPort(rabbitmq.getAmqpPort());
        cf.setUsername(rabbitmq.getAdminUsername() == null ? "guest" : rabbitmq.getAdminUsername());
        cf.setPassword(rabbitmq.getAdminPassword() == null ? "guest" : rabbitmq.getAdminPassword());
        try (Connection conn = cf.newConnection(); Channel ch = conn.createChannel()) {
            String q = "tc-test-queue";
            ch.queueDeclare(q, false, false, true, null);
            String msg = "ping";
            ch.basicPublish("", q, null, msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            com.rabbitmq.client.GetResponse resp = ch.basicGet(q, true);
            assertTrue(resp != null);
            String body = new String(resp.getBody(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals("ping", body);
        }
    }
}
