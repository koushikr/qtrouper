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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.qtrouper.core.config.QueueConfiguration;
import io.github.qtrouper.core.config.RetryConfiguration;
import io.github.qtrouper.core.config.SidelineConfiguration;
import io.github.qtrouper.core.models.QAccessInfo;
import io.github.qtrouper.core.models.QueueContext;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

public class TrouperExceptionHandlingTest extends BaseRMQSetupTest {

  @Before
  public void setUpForEachRun() {
    checkIdDependenciesAreReady();
  }

  private void checkIdDependenciesAreReady() {
    if (!rabbitMQContainer.isRunning()) {
      throw new RuntimeException("RabbitMQ is not ready to execute test");
    }
    if (rabbitConnection.getConnection() == null || rabbitConnection.getChannel() == null) {
      throw new RuntimeException("RabbitConnection is not made yet");
    }
  }

  @Test
  public void testWhenProcessThrowsException() throws Exception {
    QueueConfiguration queueConfiguration = QueueConfiguration.builder()
        .queueName("TEST_QUEUE_1")
        .concurrency(1)
        .prefetchCount(1)
        .retry(RetryConfiguration.builder()
            .ttlMs(8000)
            .backOffFactor(0)
            .maxRetries(1)
            .enabled(true)
            .build())
        .sideline(SidelineConfiguration.builder()
            .concurrency(1)
            .enabled(true)
            .build())
        .build();

    ExceptionInterface exceptionInterface = mock(ExceptionInterface.class);

    when(exceptionInterface.process()).thenThrow(new RuntimeException("test"));
    when(exceptionInterface.processSideline()).thenReturn(true);

    // Create and start a Trouper
    ExceptionTrouper trouper = new ExceptionTrouper(queueConfiguration.getQueueName(),
        queueConfiguration, rabbitConnection, QueueContext.class, Collections.emptySet(),
        exceptionInterface);
    trouper.start();

    // Publish a message to trouper
    trouper.publish(QueueContext.builder().build());

    Thread.sleep(1000);

    verify(exceptionInterface, times(2)).process();
    verify(exceptionInterface, times(1)).processSideline();
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

}
