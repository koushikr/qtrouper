package io.github.qtrouper.dw.example.config;

import io.github.qtrouper.core.config.QueueConfiguration;
import io.github.qtrouper.core.rabbit.RabbitConfiguration;
import java.util.List;

public interface ApplicationConfiguration {

  RabbitConfiguration getRabbitConfiguration();

  List<QueueConfiguration> getQueues();

}
