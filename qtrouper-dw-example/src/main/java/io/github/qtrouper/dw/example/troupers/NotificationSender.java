package io.github.qtrouper.dw.example.troupers;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.github.qtrouper.core.config.QueueConfiguration;
import io.github.qtrouper.core.models.QAccessInfo;
import io.github.qtrouper.core.models.QueueContext;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import io.github.qtrouper.managed.ManagedTrouper;
import java.util.Collections;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class NotificationSender extends ManagedTrouper<QueueContext> {

  @Inject
  public NotificationSender(
      @Named("NOTIFICATION_SENDER") QueueConfiguration config,
      RabbitConnection connection) {
    super(config.getQueueName(), config, connection, QueueContext.class, Collections.emptySet());
  }

  @Override
  public boolean process(QueueContext queueContext, QAccessInfo accessInfo) {
    log.info("Processed notification message {}", queueContext.getServiceReference());
    return true;
  }

  @Override
  public boolean processSideline(QueueContext queueContext, QAccessInfo accessInfo) {
    return process(queueContext, accessInfo);
  }
}
