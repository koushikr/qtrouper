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
package io.github.qtrouper.core.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Envelope;
import io.github.qtrouper.Trouper;
import io.github.qtrouper.Trouper.Handler;
import io.github.qtrouper.core.models.QAccessInfo;
import io.github.qtrouper.core.models.QueueContext;
import io.github.qtrouper.core.rabbit.RabbitConfiguration;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import io.github.qtrouper.utils.SerDe;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class HandlerTest {

    private Channel channel;
    private RabbitConnection rabbitConnection;
    private String mainQueueHandlerTag = "MAIN_QUEUE_HANDLER_TAG";
    private String sidelineQueueHandlerTag = "SIDELINE_QUEUE_HANDLER_TAG";
    private Long deliveryTag = 73656L;
    private Envelope envelope;
    private ArgumentCaptor<Long> deliveryTagCaptor;
    private ArgumentCaptor<Boolean> boolCaptor;

    @Before
    public void setUp() {
        channel = mock(Channel.class);
        rabbitConnection = mock(RabbitConnection.class);
        envelope = mock(Envelope.class);
        deliveryTagCaptor = ArgumentCaptor.forClass(Long.class);
        boolCaptor = ArgumentCaptor.forClass(Boolean.class);

        SerDe.init(new ObjectMapper());

        setupBaseMocks();
    }

    private void setupBaseMocks() {
        val rmqOpts = mock(Map.class);
        val rabbitConfiguration = RabbitConfiguration.builder()
            .brokers(new ArrayList<>())
            .password("")
            .userName("")
            .virtualHost("/")
            .threadPoolSize(100)
            .build();

        when(rabbitConnection.getConfig()).thenReturn(rabbitConfiguration);
        when(rabbitConnection.rmqOpts()).thenReturn(rmqOpts);
        when(rabbitConnection.newChannel()).thenReturn(channel);
        when(rabbitConnection.getConnection()).thenReturn(mock(Connection.class));
        when(rabbitConnection.channel()).thenReturn(channel);
        when(envelope.getDeliveryTag()).thenReturn(deliveryTag);
    }

    private Handler getHandlerAfterSettingUp(String queueTag,
                                             ExceptionInterface testInterface) throws IOException {
        val queueConfiguration = QueueConfiguration.builder()
            .retry(RetryConfiguration.builder()
                .enabled(false)
                .build())
            .sideline(SidelineConfiguration.builder()
                .enabled(true)
                .concurrency(1)
                .build())
            .queueName("TEST_QUEUE_1")
            .concurrency(1)
            .build();
        when(channel.basicConsume(anyString(), anyBoolean(), any()))
            .thenReturn(mainQueueHandlerTag)
            .thenReturn(sidelineQueueHandlerTag);

        // Create a trouper
        val testTrouper = new ExceptionTrouper(queueConfiguration.getQueueName(),
            queueConfiguration, rabbitConnection, QueueContext.class,
            Sets.newHashSet(KnowException.class), testInterface);
        testTrouper.start();

        // Get the handler
        return testTrouper.getHandlers().stream()
            .filter(handler -> handler.getTag().equalsIgnoreCase(queueTag))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Can't find handler"));

    }

    @Test
    public void testBasicAckAndSidelineWhenTrouperProcessThrowUnknownException() throws IOException {

        val exceptionInterface = mock(ExceptionInterface.class);
        val testHandler = getHandlerAfterSettingUp(mainQueueHandlerTag, exceptionInterface);

        when(exceptionInterface.process()).thenThrow(new RuntimeException("test exp"));

        testHandler.handleDelivery("ANY", envelope, null,
            SerDe.mapper().writeValueAsBytes(QueueContext.builder().build()));

        verify(channel).basicAck(deliveryTagCaptor.capture(), boolCaptor.capture());
        Assert.assertEquals(deliveryTag, deliveryTagCaptor.getValue());
        Assert.assertEquals(false, boolCaptor.getValue());
        verify(channel, times(1)).basicPublish(any(), any(), any(), any());
    }

    @Test
    public void testBasicAckWhenTrouperSidelineIsSuccess() throws IOException {
        val exceptionInterface = mock(ExceptionInterface.class);
        val testHandler = getHandlerAfterSettingUp(sidelineQueueHandlerTag, exceptionInterface);

        when(exceptionInterface.processSideline()).thenReturn(true);

        testHandler.handleDelivery("ANY", envelope, null,
            SerDe.mapper().writeValueAsBytes(QueueContext.builder().build()));

        verify(channel).basicAck(deliveryTagCaptor.capture(), boolCaptor.capture());
        Assert.assertEquals(deliveryTag, deliveryTagCaptor.getValue());
        Assert.assertEquals(false, boolCaptor.getValue());
    }

    @Test
    public void testBasicRejectWhenTrouperSidelineIsFailure() throws IOException {
        val exceptionInterface = mock(ExceptionInterface.class);
        val testHandler = getHandlerAfterSettingUp(sidelineQueueHandlerTag, exceptionInterface);

        when(exceptionInterface.processSideline()).thenReturn(false);

        testHandler.handleDelivery("ANY", envelope, null,
            SerDe.mapper().writeValueAsBytes(QueueContext.builder().build()));

        verify(channel).basicReject(deliveryTagCaptor.capture(), boolCaptor.capture());
        Assert.assertEquals(deliveryTag, deliveryTagCaptor.getValue());
        Assert.assertEquals(true, boolCaptor.getValue());
    }

    @Test
    public void testBasicAckWhenTrouperSidelineThrowsKnowException() throws IOException {
        val exceptionInterface = mock(ExceptionInterface.class);
        val testHandler = getHandlerAfterSettingUp(sidelineQueueHandlerTag, exceptionInterface);

        when(exceptionInterface.processSideline()).thenThrow(new KnowException());

        testHandler.handleDelivery("ANY", envelope, null,
            SerDe.mapper().writeValueAsBytes(QueueContext.builder().build()));

        verify(channel).basicAck(deliveryTagCaptor.capture(), boolCaptor.capture());
        Assert.assertEquals(deliveryTag, deliveryTagCaptor.getValue());
        Assert.assertEquals(false, boolCaptor.getValue());
    }

    @Test
    public void testBasicAckWhenTrouperSidelineThrowsUnKnowException() throws IOException {
        val exceptionInterface = mock(ExceptionInterface.class);
        val testHandler = getHandlerAfterSettingUp(sidelineQueueHandlerTag, exceptionInterface);

        when(exceptionInterface.processSideline()).thenThrow(new RuntimeException());

        testHandler.handleDelivery("ANY", envelope, null,
            SerDe.mapper().writeValueAsBytes(QueueContext.builder().build()));

        verify(channel).basicReject(deliveryTagCaptor.capture(), boolCaptor.capture());
        Assert.assertEquals(deliveryTag, deliveryTagCaptor.getValue());
        Assert.assertEquals(true, boolCaptor.getValue());
    }

    @Test
    public void testBasicAckWhenTrouperProcessIsSuccess() throws IOException {
        val exceptionInterface = mock(ExceptionInterface.class);
        val testHandler = getHandlerAfterSettingUp(mainQueueHandlerTag, exceptionInterface);

        when(exceptionInterface.process()).thenReturn(true);

        testHandler.handleDelivery("ANY", envelope, null,
            SerDe.mapper().writeValueAsBytes(QueueContext.builder().build()));

        verify(channel).basicAck(deliveryTagCaptor.capture(), boolCaptor.capture());
        Assert.assertEquals(deliveryTag, deliveryTagCaptor.getValue());
        Assert.assertEquals(false, boolCaptor.getValue());
    }

    @Test
    public void testBasicAckAndSidelineWhenTrouperProcessIsFailure() throws IOException {
        val exceptionInterface = mock(ExceptionInterface.class);
        val testHandler = getHandlerAfterSettingUp(mainQueueHandlerTag, exceptionInterface);

        when(exceptionInterface.process()).thenReturn(false);

        testHandler.handleDelivery("ANY", envelope, null,
            SerDe.mapper().writeValueAsBytes(QueueContext.builder().build()));

        verify(channel).basicAck(deliveryTagCaptor.capture(), boolCaptor.capture());
        Assert.assertEquals(deliveryTag, deliveryTagCaptor.getValue());
        Assert.assertEquals(false, boolCaptor.getValue());
        verify(channel, times(1)).basicPublish(any(), any(), any(), any());
    }

    @Test
    public void testBasicAckWhenTrouperProcessThrowsKnowException() throws IOException {
        val exceptionInterface = mock(ExceptionInterface.class);
        val testHandler = getHandlerAfterSettingUp(mainQueueHandlerTag, exceptionInterface);

        when(exceptionInterface.process()).thenThrow(new KnowException());

        testHandler.handleDelivery("ANY", envelope, null,
            SerDe.mapper().writeValueAsBytes(QueueContext.builder().build()));

        verify(channel).basicAck(deliveryTagCaptor.capture(), boolCaptor.capture());
        Assert.assertEquals(deliveryTag, deliveryTagCaptor.getValue());
        Assert.assertEquals(false, boolCaptor.getValue());
    }

    static class ExceptionTrouper extends Trouper<QueueContext> {

        ExceptionInterface testInterface;

        protected ExceptionTrouper(String queueName, QueueConfiguration config,
            RabbitConnection connection, Class<? extends QueueContext> clazz,
            Set<Class<?>> droppedExceptionTypes, ExceptionInterface testInterface) {
            super(queueName, config, connection, clazz, droppedExceptionTypes);
            this.testInterface = testInterface;
        }

        @Override
        public boolean process(QueueContext queueContext, QAccessInfo accessInfo) {
            return testInterface.process();
        }

        @Override
        public boolean processSideline(QueueContext queueContext, QAccessInfo accessInfo) {
            return testInterface.processSideline();
        }
    }

    interface ExceptionInterface {
        boolean process();
        boolean processSideline();
    }

    static class KnowException extends RuntimeException {
    }

}
