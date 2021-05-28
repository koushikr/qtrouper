package io.github.qtrouper.healthchecks;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheck.Result;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TrouperHealthCheckTest {

  private RabbitConnection rabbitConnection;
  private TrouperHealthCheck trouperHealthCheck;

  @BeforeEach
  public void setUp() {
    rabbitConnection = mock(RabbitConnection.class);
    trouperHealthCheck = new TrouperHealthCheck(rabbitConnection);
  }

  @Test
  public void testWhenConnectionIsNull() throws Exception {
    when(rabbitConnection.getConnection()).thenReturn(null);

    Result result = trouperHealthCheck.check();

    Assertions.assertFalse(result.isHealthy());
    Assertions.assertEquals("Not Connected", result.getMessage());
  }

  @Test
  public void testWhenConnectionIsNotOpen() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.isOpen()).thenReturn(false);
    when(rabbitConnection.getConnection()).thenReturn(connection);

    Result result = trouperHealthCheck.check();

    Assertions.assertFalse(result.isHealthy());
    Assertions.assertEquals("Not Connected", result.getMessage());
  }

  @Test
  public void testWhenConnectionIsOpenButExceptionWithChannel() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.isOpen()).thenReturn(true);
    when(connection.createChannel()).thenThrow(new IOException());
    when(rabbitConnection.getConnection()).thenReturn(connection);

    Result result = trouperHealthCheck.check();

    Assertions.assertFalse(result.isHealthy());
    Assertions.assertEquals("Connection is open, could not create channel", result.getMessage());
  }

  @Test
  public void testWhenConnectionIsOpenAndOperational() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.isOpen()).thenReturn(true);
    Channel channel = mock(Channel.class);
    when(connection.createChannel()).thenReturn(channel);
    when(rabbitConnection.getConnection()).thenReturn(connection);

    Result result = trouperHealthCheck.check();

    Assertions.assertTrue(result.isHealthy());
  }

}