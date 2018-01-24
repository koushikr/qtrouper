package com.github.qtrouper.core.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.github.qtrouper.core.rabbit.RabbitConnection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import javax.inject.Singleton;
import java.io.IOException;

/**
 * @author koushik
 */
@Singleton
public class TrouperHealthCheck extends HealthCheck{
    private final RabbitConnection rabbitConnection;

    public TrouperHealthCheck(RabbitConnection rabbitConnection) {
        this.rabbitConnection = rabbitConnection;
    }

    @Override
    protected Result check() throws Exception {
        final Connection connection = rabbitConnection.getConnection();
        Result result;
        if (connection != null && connection.isOpen()) {
            try {
                Channel channel = connection.createChannel();
                channel.close();
                result = Result.healthy();
            } catch (IOException e) {
                result = Result.unhealthy("Connection is open, could not create channel");
            }
        } else {
            result = Result.unhealthy("Not Connected");
        }
        return result;
    }
}
