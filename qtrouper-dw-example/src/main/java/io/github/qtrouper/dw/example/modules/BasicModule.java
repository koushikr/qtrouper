package io.github.qtrouper.dw.example.modules;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.dropwizard.Configuration;
import io.github.qtrouper.TrouperBundle;
import io.github.qtrouper.core.config.QueueConfiguration;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import io.github.qtrouper.dw.example.config.ApplicationConfiguration;
import ru.vyarus.dropwizard.guice.module.support.DropwizardAwareModule;

public class BasicModule<Conf extends Configuration & ApplicationConfiguration> extends DropwizardAwareModule<Conf> {

  private final TrouperBundle<Conf> trouperBundle;

  public BasicModule(TrouperBundle<Conf> trouperBundle) {
    this.trouperBundle = trouperBundle;
  }

  @Override
  protected void configure() {
  }

  @Provides
  @Singleton
  public RabbitConnection getRabbitConnection() {
    return trouperBundle.getRabbitConnection();
  }

  @Provides
  @Singleton
  @Named("NOTIFICATION_SENDER")
  public QueueConfiguration getNotificationSenderConf() {
    return configuration().getQueues().get(0);
  }

  @Provides
  @Singleton
  @Named("ACKNOWLEDGER")
  public QueueConfiguration getAcknowledgerConf() {
    return configuration().getQueues().get(1);
  }
}
