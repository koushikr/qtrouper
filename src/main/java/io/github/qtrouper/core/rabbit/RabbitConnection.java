/*
 * Copyright 2019 Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.qtrouper.core.rabbit;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.StandardMetricsCollector;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author koushik
 */
@Slf4j
@Singleton
@Getter
public class RabbitConnection {

    private final RabbitConfiguration config;
    private Connection connection;
    private Channel channel;
    private MetricRegistry metricRegistry;

    public RabbitConnection(RabbitConfiguration rabbitConfiguration,
                            MetricRegistry metricRegistry) {
        this.config = rabbitConfiguration;
        this.metricRegistry = metricRegistry;
    }

    /**
     * Starts the RabbitMQ Connection.
     * Sets the required username, password and other connection settings.
     * Creates both the connection and a default channel which would later be used for publish
     * @throws Exception
     */
    public void start() throws Exception {
        log.info("Starting Rabbit Connection");

        ConnectionFactory factory = new ConnectionFactory();

        if (!Strings.isNullOrEmpty(config.getUserName())) factory.setUsername(config.getUserName());
        if (!Strings.isNullOrEmpty(config.getPassword())) factory.setPassword(config.getPassword());
        if (!Strings.isNullOrEmpty(config.getVirtualHost())) factory.setVirtualHost(config.getVirtualHost());

        if(config.isSslEnabled()){
           factory.useSslProtocol();
        }

        if (config.isMetricsEnabled()) {
            factory.setMetricsCollector(new StandardMetricsCollector(metricRegistry));
        }
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

    /**
     * Destroys the channel and connection.
     * Gets triggered during shutdown
     * @throws Exception
     */
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
