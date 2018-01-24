package com.github.qtrouper.rabbit;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.dropwizard.lifecycle.Managed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * @author koushik
 */
@Slf4j
@Singleton
@Getter
public class RabbitConnection implements Managed{

    private final RabbitConfiguration config;
    private Connection connection;
    private Channel channel;

    public RabbitConnection(RabbitConfiguration rabbitConfiguration) throws Exception {
        this.config = rabbitConfiguration;

        this.start();
    }

    public void start() throws Exception {
        log.info("Starting Rabbit Connection");

        ConnectionFactory factory = new ConnectionFactory();

        if (!Strings.isNullOrEmpty(config.getUserName())) factory.setUsername(config.getUserName());
        if (!Strings.isNullOrEmpty(config.getPassword())) factory.setPassword(config.getPassword());
        if (!Strings.isNullOrEmpty(config.getVirtualHost())) factory.setVirtualHost(config.getVirtualHost());

        factory.setAutomaticRecoveryEnabled(true);
        factory.setTopologyRecoveryEnabled(true);
        factory.setNetworkRecoveryInterval(3000);
        factory.setRequestedHeartbeat(60);

        connection = factory.newConnection(Executors.newFixedThreadPool(config.getThreadPoolSize()), config.getBrokers()
                .stream()
                .map(broker -> new Address(broker.getHost(), broker.getPort())).toArray(Address[]::new));

        channel = connection.createChannel();

        log.info("Started Rabbit Connection");
    }

    public void stop() throws Exception {
        if (null != channel && channel.isOpen()) {
            channel.close();
        }
        if (null != connection && connection.isOpen()) {
            connection.close();
        }
    }

    public Channel channel() {
        return channel;
    }

    public Channel newChannel() throws IOException {
        return connection.createChannel();
    }

    public void ensure(final String queueName,
                       final String exchange,
                       final Map<String, Object> rmqOpts) throws Exception {
        ensure(queueName, queueName, exchange, rmqOpts);
    }

    public void ensure(final String queueName,
                       final String routingQueue,
                       final String exchange) throws Exception {
        ensure(queueName, routingQueue, exchange, rmqOpts());
    }

    public void ensure(final String queueName,
                       final String routingQueue,
                       final String exchange,
                       final Map<String, Object> rmqOpts) throws Exception {
        channel.queueDeclare(queueName, true, false, false, rmqOpts);
        channel.queueBind(queueName, exchange, routingQueue);
        log.info("Created queue: {}", queueName);
    }

    public Map<String, Object> rmqOpts() {
        return ImmutableMap.<String, Object>builder()
                .put("x-ha-policy", "all")
                .put("ha-mode", "all")
                .build();
    }

    public Map<String, Object> rmqOpts(String deadLetterExchange, String routingKey) {
        return ImmutableMap.<String, Object>builder()
                .put("x-ha-policy", "all")
                .put("ha-mode", "all")
                .put("x-dead-letter-exchange", deadLetterExchange)
                .put("x-dead-letter-routing-key", routingKey)
                .build();
    }



}
