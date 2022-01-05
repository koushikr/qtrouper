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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.github.qtrouper.core.rabbit.RabbitBroker;
import io.github.qtrouper.core.rabbit.RabbitConfiguration;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import io.github.qtrouper.utils.SerDe;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.testcontainers.containers.RabbitMQContainer;

@Slf4j
public abstract class BaseRMQSetupTest {

  protected static RabbitMQContainer rabbitMQContainer;
  protected static RabbitConnection rabbitConnection;

  @BeforeClass
  public static void setUp() throws Exception {
    // Create and start rabbitmq-docker-instance
    rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-alpine");
    rabbitMQContainer.start();

    // Create RabbitConnection for troupers
    rabbitConnection = new RabbitConnection(getRabbitConfiguration());
    rabbitConnection.start();

    SerDe.init(new ObjectMapper());
  }

  @AfterClass
  public static void tearDown() throws Exception {
    rabbitConnection.stop();
    rabbitMQContainer.stop();
  }

  protected void waitTillQueueIsEmpty(String queueName) throws IOException, InterruptedException {
    int count = 0;
    int maxCount = 100;
    while (true) {
      long currentCount = rabbitConnection.getChannel().messageCount(queueName);
      log.info("Current queue size for {} is {}", queueName, currentCount);
      if (currentCount == 0) {
        return;
      }
      Thread.sleep(100);
      count++;
      if (count == maxCount) {
        throw new RuntimeException("Queues not getting cleared in the test");
      }
    }
  }

  private static RabbitConfiguration getRabbitConfiguration() {
    return RabbitConfiguration.builder()
        .brokers(Lists.newArrayList(RabbitBroker.builder()
            .host(rabbitMQContainer.getHost())
            .port(rabbitMQContainer.getAmqpPort())
            .build()))
        .threadPoolSize(10)
        .userName("guest")
        .password("guest")
        .virtualHost("/")
        .build();
  }

}
