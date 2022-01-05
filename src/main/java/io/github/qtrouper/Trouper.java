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
package io.github.qtrouper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.github.qtrouper.core.config.QueueConfiguration;
import io.github.qtrouper.core.models.QAccessInfo;
import io.github.qtrouper.core.models.QueueContext;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import io.github.qtrouper.utils.SerDe;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * @author koushik
 */
@Data
@EqualsAndHashCode
@ToString
@Slf4j
@SuppressWarnings("unused")
public abstract class Trouper<C extends QueueContext> {

    private static final String RETRY_COUNT = "x-retry-count";
    private static final String EXPIRATION = "x-message-ttl";
    private static final String EXPIRES_AT_TIMESTAMP = "x-expires-timestamp";
    private static final String EXPIRES_AT_ENABLED = "x-expires-enabled";
    private static final String CONTENT_TYPE = "text/plain";

    private final QueueConfiguration config;
    private final RabbitConnection connection;
    private final Class<? extends C> clazz;
    private final Set<Class<?>> droppedExceptionTypes;
    private final int prefetchCount;
    private final String queueName;
    private Channel publishChannel;
    private List<Handler> handlers = Lists.newArrayList();

    protected Trouper(
            String queueName,
            QueueConfiguration config,
            RabbitConnection connection,
            Class<? extends C> clazz,
            Set<Class<?>> droppedExceptionTypes) {
        this.config = config;
        this.connection = connection;
        this.clazz = clazz;
        this.droppedExceptionTypes = droppedExceptionTypes;
        this.prefetchCount = config.getPrefetchCount();
        this.queueName = String.format("%s.%s", config.getNamespace(), queueName);
    }


    public abstract boolean process(C queueContext, QAccessInfo accessInfo);

    public abstract boolean processSideline(C queueContext, QAccessInfo accessInfo);

    /*
     * Determines if a message is expired. Checks if message expiry is enabled and current time is more than expiry time.
     * @param expiresAtEnabled  {@link Boolean}         Whether message expiry is enabled or not
     * @param expiresAt         {@link Long}            The time at which the messsage is set to expire
     */
    private boolean isMessageExpired(boolean expiresAtEnabled, long expiresAt){
        return expiresAtEnabled && expiresAt != 0 && expiresAt < System.currentTimeMillis();
    }

    /*
     * Handle does the following things.
     *
     * Calls the appropriate process method on the consumer.
     * If the process method succeeds, returns true and exits.
     * If otherwise, checks if retry is enabled.
     * If retry ain't enabled, publishes to sideline and returns
     * If retry is enabled, checks the current count for retry.
     * If retryCount is greater than maxRetries, publishes to sideline and exits
     * If otherwise, increments the retryCount and publishes into retryQueue : which would further deadLetter into mainQueue after ttl.
     * @param queueContext      {@link C}                 The queueContext that is associated with the Trouper
     * @param properties        {@link AMQP.BasicProperties}    The AMQP Basic Properties
     * @return  if the handle is successful or otherwise.
     */
    @SneakyThrows
    private boolean handle(C queueContext, AMQP.BasicProperties properties) {
        val expiresAtEnabled = (Boolean) properties.getHeaders().getOrDefault(EXPIRES_AT_ENABLED, false);
        val expiresAt = (Long) properties.getHeaders().getOrDefault(EXPIRES_AT_TIMESTAMP, 0L);

        if (isMessageExpired(expiresAtEnabled, expiresAt)){
            log.info("Ignoring queueContext due to expiry {}", queueContext);
            return true;
        }

        try {
            val processed = process(queueContext, getAccessInformation(properties));
            if(processed) return true;
        } catch (Exception ex) {
            log.error("Exception while processing the queueContext {}", queueContext, ex);
        }

        val retry = config.getRetry();
        if (retry.isEnabled()) {
            var retryCount = (int) properties.getHeaders().getOrDefault(RETRY_COUNT, 0);
            if (retryCount >= retry.getMaxRetries()) {
                sidelinePublish(queueContext);
                return true;
            }
            retryCount++;
            val expiration = (long) properties.getHeaders().getOrDefault(EXPIRATION, retry.getTtlMs());
            val newExpiration = expiration * retry.getBackOffFactor();
            retryPublishWithExpiry(queueContext, retryCount, newExpiration, expiresAt, expiresAtEnabled);
        }else{
            sidelinePublish(queueContext);
        }
        return true;
    }

    private QAccessInfo getAccessInformation(AMQP.BasicProperties properties) {
        val retryCount = (int) properties.getHeaders().getOrDefault(RETRY_COUNT, 0);
        return QAccessInfo.builder()
                .retryCount(retryCount)
                .idempotencyCheckRequired(retryCount > 0)
                .build();
    }

    private String getRetryExchange() {
        return this.config.getNamespace() + "_RETRY";
    }

    private String getSidelineQueue() {
        return queueName + "_SIDELINE";
    }

    private String getRetryQueue() {
        return queueName + "_RETRY";
    }

    public final void publish(C c) {
        publish(c, new AMQP.BasicProperties.Builder().contentType(CONTENT_TYPE).deliveryMode(2).headers(new HashMap<>()).build());
    }

    /**
     * Publish messages which gets expired at given timestamp if expiration is enabled
     *
     * @param queueContext           {@link C}             The queueContext which gets published
     * @param expiresAt         {@link Long}                The timestamp at which a queueContext gets expired if expiration is enabled
     * @param expiresAtEnabled  {@link Boolean}             A flag to determine if queueContext expiration is enabled or not
     */
    public final void publishWithExpiry(C queueContext, long expiresAt, boolean expiresAtEnabled) {
      publish(queueContext, ImmutableMap.of(EXPIRES_AT_TIMESTAMP, expiresAt, EXPIRES_AT_ENABLED, expiresAtEnabled));
    }

    public final void publish(C queueContext, Map<String, Object> headers) {
        publish(queueContext, new AMQP.BasicProperties.Builder().contentType(CONTENT_TYPE).deliveryMode(2).headers(headers).build());
    }

    @SneakyThrows
    private void publish(C queueContext, AMQP.BasicProperties properties) {
        log.info("Publishing to queue {}: with context {}", queueName, queueContext);
        publishChannel.basicPublish(this.config.getNamespace(), queueName, properties, SerDe.mapper().writeValueAsBytes(queueContext));
        log.info("Published to queue {}: with context {}", queueName, queueContext);
    }

    @SneakyThrows
    public void sidelinePublish(C queueContext) {
        log.info("Publishing to {}: {}", getSidelineQueue(), queueContext);
        publishChannel.basicPublish(this.config.getNamespace(), getSidelineQueue(),  new AMQP.BasicProperties.Builder().contentType(CONTENT_TYPE).deliveryMode(2).headers(ImmutableMap.of()).build(), SerDe.mapper().writeValueAsBytes(queueContext));
        log.info("Published to {}: {}", getSidelineQueue(), queueContext);
    }

    /**
     * Sets the retryCount and expiration and publishes into the retry queue
     * which would further deadLetter into the mainQueue.
     * @param queueContext           {@link C}     The queueContext that is associated with the Trouper
     * @param retryCount        {@link Integer}     The currentRetryCount of the queueContext
     * @param expiration        {@link Long}        The current expiration in milliseconds
     */
    public final void retryPublish(C queueContext, int retryCount, long expiration){
        val properties = new AMQP.BasicProperties.Builder()
                .contentType(CONTENT_TYPE)
                .expiration(String.valueOf(expiration))
                .deliveryMode(2)
                .headers(ImmutableMap.of(RETRY_COUNT, retryCount, EXPIRATION, expiration))
                .build();
        retryPublish(queueContext, properties);
    }

    /**
     * Sets the retryCount, expiration, expiryTimestamp and publishes into the retry queue which would further
     * deadLetter into the mainQueue.
     *
     * @param queueContext {@link C}     The c that is associated with the Trouper
     * @param retryCount {@link Integer}     The currentRetryCount of the c
     * @param expiration {@link Long}        The current expiration in milliseconds
     * @param expiresAt {@link Long}        The timestamp at which the c should expire
     * @param expiresAtEnabled {@link Boolean}     Flag to determine whether the c should expire at expiresAt timestamp
     */
    public final void retryPublishWithExpiry(C queueContext, int retryCount, long expiration,
                                             long expiresAt, boolean expiresAtEnabled) {
      val properties = new AMQP.BasicProperties.Builder()
              .contentType(CONTENT_TYPE)
              .expiration(String.valueOf(expiration))
              .deliveryMode(2)
              .headers(ImmutableMap.of(RETRY_COUNT, retryCount, EXPIRATION, expiration, EXPIRES_AT_ENABLED, expiresAtEnabled, EXPIRES_AT_TIMESTAMP, expiresAt))
              .build();
      retryPublish(queueContext, properties);
    }

    @SneakyThrows
    private void retryPublish(C queueContext, AMQP.BasicProperties properties) {
      log.info("Publishing to {}: {}", getRetryQueue(), queueContext);
      connection.getChannel().basicPublish(
          getRetryExchange(),
          getRetryQueue(),
          properties, SerDe.mapper().writeValueAsBytes(queueContext)
      );
      log.info("Published to {}: {}", getRetryQueue(), queueContext);
    }

    @SneakyThrows
    private void ensureExchange(String exchange) {
        connection.channel().exchangeDeclare(
                exchange,
                "direct",
                true,
                false,
                ImmutableMap.<String, Object>builder()
                        .put("x-ha-policy", "all")
                        .put("ha-mode", "all")
                        .build());
    }

    @SneakyThrows
    private void addHandler(int consumerNumber, boolean sideline) {
        val consumeChannel = connection.newChannel();
        val handler = new Handler(consumeChannel,
                clazz, prefetchCount, this, sideline);
        val tag = consumeChannel.basicConsume(sideline ? getSidelineQueue() : queueName, false, handler);
        handler.setTag(tag);
        handlers.add(handler);
        log.info("Started  consumer {} with queueName {}", consumerNumber, queueName);
    }

    /**
     * Creates the required exchanges and queues.
     *
     * Creates a mainExchange and a retryExchange, with retryExchange dead lettering into the mainExchange
     * Creates main queues and sideline queues on mainExchange and retryQueues on retryExchange
     *
     * Binds the consumers on both main and sideline queues depending on the appropriate configuration
     * settings defined.
     */
    public void start() {
        val exchange = this.config.getNamespace();
        val dlExchange = getRetryExchange();
        ensureExchange(exchange);
        ensureExchange(dlExchange);
        this.publishChannel = connection.newChannel();
        connection.ensure(queueName, this.config.getNamespace(), connection.rmqOpts());
        connection.ensure(getRetryQueue(), dlExchange, connection.rmqOpts(exchange, queueName));
        connection.ensure(getSidelineQueue(), this.config.getNamespace(), connection.rmqOpts());
        if (config.isConsumerEnabled()) {
            IntStream.rangeClosed(1, config.getConcurrency()).forEach(i -> addHandler(i, false));
            val sidelineConfiguration = config.getSideline();
            if (sidelineConfiguration.isEnabled()) {
                IntStream.rangeClosed(1, sidelineConfiguration.getConcurrency()).forEach(i -> addHandler(i, true));
            }
        }
    }

    public void stop() {
        try {
            publishChannel.close();
        } catch (Exception e) {
            log.error(String.format("Error closing publisher:%s", queueName), e);
        }
        handlers.forEach(handler -> {
            try {
                val channel = handler.getChannel();
                channel.basicCancel(handler.getTag());
                channel.close();
            } catch (Exception e) {
                log.error(String.format("Error cancelling consumer: %s", handler.getTag()), e);
            }
        });
    }

    public class Handler extends DefaultConsumer {

        private final Class<? extends C> clazz;
        private final Trouper<C> trouper;
        private final boolean sideline;

        @Getter
        @Setter
        private String tag;

        private Handler(Channel channel,
                        Class<? extends C> clazz,
                        int prefetchCount,
                        Trouper<C> trouper,
                        boolean sideline) throws IOException {
            super(channel);

            this.clazz = clazz;
            this.trouper = trouper;
            this.sideline = sideline;
            getChannel().basicQos(prefetchCount);
        }

        private boolean isExceptionIgnorable(Throwable t) {
            return droppedExceptionTypes
                    .stream()
                    .anyMatch(exceptionType -> ClassUtils.isAssignable(t.getClass(), exceptionType));
        }

        /**
         * Need to augment the properties with checks and balances for people might push into the queue async, w/o any header
         * information. Want trouper to gracefully handle such a scenario.
         *
         * @param basicProperties           {@link AMQP.BasicProperties}    The properties object sent during the push.
         */
        private AMQP.BasicProperties getProperties(AMQP.BasicProperties basicProperties){
            if(null == basicProperties){
                return new AMQP.BasicProperties.Builder().contentType(CONTENT_TYPE).deliveryMode(2).headers(new HashMap<>()).build();
            }

            if(null == basicProperties.getHeaders()){
                return basicProperties.builder().headers(new HashMap<>()).build();
            }

            return basicProperties;
        }

        /**
         * Understands if the consumer is for the mainQueue or the sideline Queue
         * Calls appropriate process methods.
         *
         * If message handling is successful, acknowledges the connection with multiple re-queues set to false.
         * If message handling is unsuccessful, rejects the message setting requeue to true.
         *
         * In case of any exceptions, checks if any of the exceptions are whitelisted, and repeats the above.
         *
         * @param consumerTag       {@link String}              The consumerTag associated with the message
         * @param envelope          {@link Envelope}            RabbitMQ Envelope object
         * @param properties        {@link AMQP.BasicProperties}AMQP BasicProperties associated with the message
         * @param body              {@link Byte[]}              ByteArray representing the message
         */
        @Override
        @SneakyThrows
        public void handleDelivery(String consumerTag, Envelope envelope,
                                   AMQP.BasicProperties properties, byte[] body) {
            try {
                val queueContext = SerDe.mapper().readValue(body, clazz);
                val propertyDetails = getProperties(properties);
                val success = sideline ?
                        trouper.processSideline(queueContext, getAccessInformation(propertyDetails)) :
                        trouper.handle(queueContext, propertyDetails);

                if (success) {
                    getChannel().basicAck(envelope.getDeliveryTag(), false);
                } else {
                    getChannel().basicReject(envelope.getDeliveryTag(), true);
                }
            } catch (Throwable t) {
                log.error("Error processing message with tag {}, routing key {} and throwable {}", envelope.getDeliveryTag(), envelope.getRoutingKey(), t);
                if (isExceptionIgnorable(t)) {
                    log.warn("Acked message due to exception: ", t);
                    getChannel().basicAck(envelope.getDeliveryTag(), false);
                } else {
                    getChannel().basicReject(envelope.getDeliveryTag(), true);
                }
            }
        }
    }
}
