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
package io.github.qtrouper.managed;

import io.dropwizard.lifecycle.Managed;
import io.github.qtrouper.Trouper;
import io.github.qtrouper.core.config.QueueConfiguration;
import io.github.qtrouper.core.models.QAccessInfo;
import io.github.qtrouper.core.models.QueueContext;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ManagedTrouper<Message extends QueueContext>
    extends Trouper<Message>
    implements Managed {

  public ManagedTrouper(
      String queueName,
      QueueConfiguration config,
      RabbitConnection connection,
      Class<? extends Message> clazz,
      Set<Class<?>> droppedExceptionTypes) {
    super(queueName, config, connection, clazz, droppedExceptionTypes);
  }

  @Override
  public abstract boolean process(Message message, QAccessInfo accessInfo);

  @Override
  public abstract boolean processSideline(Message message, QAccessInfo accessInfo);

  @Override
  public void start() throws Exception {
    log.info("Starting trouper : {}", getQueueName());
    super.start();
    log.debug("Started trouper : {}", getQueueName());
  }

  @Override
  public void stop() {
    log.info("Stopping trouper : {}", getQueueName());
    super.stop();
    log.debug("Starting trouper : {}", getQueueName());
  }
}
