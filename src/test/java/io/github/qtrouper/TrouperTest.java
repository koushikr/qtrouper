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


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.github.qtrouper.core.config.QueueConfiguration;
import io.github.qtrouper.core.config.RetryConfiguration;
import io.github.qtrouper.core.config.SidelineConfiguration;
import io.github.qtrouper.core.models.QAccessInfo;
import io.github.qtrouper.core.models.QueueContext;
import io.github.qtrouper.core.rabbit.RabbitConfiguration;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * @author koushik
 */
public class TrouperTest {

    class TestTrouper extends Trouper<QueueContext>{

        protected TestTrouper(String queueName, QueueConfiguration config, RabbitConnection connection, Class<? extends QueueContext> clazz, Set<Class<?>> droppedExceptionTypes) {
            super(queueName, config, connection, clazz, droppedExceptionTypes);
        }

        @Override
        public boolean process(QueueContext queueContext, QAccessInfo accessInfo) {
            return true;
        }

        @Override
        public boolean processSideline(QueueContext queueContext, QAccessInfo accessInfo) {
            return true;
        }
    }

    private Connection connection = mock(Connection.class);
    private Channel channel = mock(Channel.class);

    private static final String DEFAULT_NAMESPACE = "default";

    private RabbitConnection rabbitConnection;

    private RetryConfiguration getRetryConfiguration(boolean enabled, int maxRetries){
        return RetryConfiguration.builder()
                .enabled(enabled)
                .maxRetries(maxRetries)
                .ttlMs(100)
                .build();
    }

    private SidelineConfiguration getSidelineConfiguration(boolean enabled, int concurrency){
        return SidelineConfiguration.builder()
                .enabled(enabled)
                .concurrency(concurrency)
                .build();
    }

    @BeforeEach
    public void setup() throws IOException {
        this.rabbitConnection = mock(RabbitConnection.class);
    }

    private TestTrouper getTrouperAfterStart(QueueConfiguration queueConfiguration) throws Exception {
        Map<String, Object> rmqOpts = mock(Map.class);
        RabbitConfiguration rabbitConfiguration = RabbitConfiguration.builder()
                .brokers(new ArrayList<>())
                .password("")
                .userName("")
                .virtualHost("/")
                .threadPoolSize(100)
                .build();

        when(rabbitConnection.getConfig()).thenReturn(rabbitConfiguration);
        when(rabbitConnection.rmqOpts()).thenReturn(rmqOpts);
        when(rabbitConnection.newChannel()).thenReturn(channel);
        when(rabbitConnection.getConnection()).thenReturn(connection);
        when(rabbitConnection.channel()).thenReturn(channel);


        TestTrouper testTrouper = new TestTrouper(queueConfiguration.getQueueName(), queueConfiguration,
                rabbitConnection, QueueContext.class, new HashSet<>());

        testTrouper.start();


        verify(channel, times(2)).exchangeDeclare(
                anyString(), anyString(), anyBoolean(), anyBoolean(), any()
        );

        verify(rabbitConnection, times(3)).ensure(anyString(), anyString(), any(Map.class));

        return testTrouper;
    }

    @Test
    public void trouperStartTestWithNoConsumers() throws Exception {
        QueueConfiguration queueConfiguration = QueueConfiguration.builder()
                .queueName("queue")
                .namespace(DEFAULT_NAMESPACE)
                .concurrency(0)
                .prefetchCount(10)
                .consumerDisabled(true)
                .build();

        getTrouperAfterStart(queueConfiguration);
    }

    @Test
    public void trouperStartTestWithNoRetryAndSideline() throws Exception {
        QueueConfiguration queueConfiguration = QueueConfiguration.builder()
                .queueName("queue")
                .namespace(DEFAULT_NAMESPACE)
                .concurrency(0)
                .prefetchCount(10)
                .consumerDisabled(false)
                .retry(getRetryConfiguration(false, 10))
                .sideline(getSidelineConfiguration(false, 0))
                .build();

        getTrouperAfterStart(queueConfiguration);
    }

    @Test
    public void trouperStartTestWithOnlySideline() throws Exception {
        QueueConfiguration queueConfiguration = QueueConfiguration.builder()
                .queueName("queue")
                .namespace(DEFAULT_NAMESPACE)
                .concurrency(0)
                .prefetchCount(10)
                .consumerDisabled(false)
                .retry(getRetryConfiguration(false, 10))
                .sideline(getSidelineConfiguration(true, 1))
                .build();

        when(channel.basicConsume(anyString(), anyBoolean(), any())).thenReturn("tag");

        Trouper trouper = getTrouperAfterStart(queueConfiguration);

        Assertions.assertTrue(trouper.getHandlers().size() == 1);
    }

    @Test
    public void trouperStartWithAllEncompassingConfig() throws Exception {
        QueueConfiguration queueConfiguration = QueueConfiguration.builder()
                .queueName("queue")
                .namespace(DEFAULT_NAMESPACE)
                .concurrency(10)
                .prefetchCount(10)
                .consumerDisabled(false)
                .retry(getRetryConfiguration(false, 10))
                .sideline(getSidelineConfiguration(true, 10))
                .build();

        Trouper trouper = getTrouperAfterStart(queueConfiguration);

        Assertions.assertTrue(trouper.getHandlers().size() == 20);
    }
}
