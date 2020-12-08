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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QueueConfigurationTest {

  @Test
  public void testQueueConfigurationDefaultViaConstructor() {


    QueueConfiguration queueConfiguration = new QueueConfiguration();

    Assertions.assertFalse(queueConfiguration.isConsumerDisabled());
    Assertions.assertEquals(queueConfiguration.getConcurrency(), 3);
    Assertions.assertEquals(queueConfiguration.getNamespace(), "qtrouper");


  }

  @Test
  public void testQueueConfigurationDefaultViaBuilder() {

    QueueConfiguration queueViaBuilder = QueueConfiguration.builder().build();

    Assertions.assertFalse(queueViaBuilder.isConsumerDisabled());
    Assertions.assertEquals(queueViaBuilder.getConcurrency(), 3);
    Assertions.assertEquals(queueViaBuilder.getNamespace(), "qtrouper");

  }

}