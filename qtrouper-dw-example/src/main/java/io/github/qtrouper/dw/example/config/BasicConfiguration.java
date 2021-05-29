package io.github.qtrouper.dw.example.config;

import io.dropwizard.Configuration;
import io.github.qtrouper.core.config.QueueConfiguration;
import io.github.qtrouper.core.rabbit.RabbitConfiguration;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BasicConfiguration extends Configuration implements ApplicationConfiguration {

  @Valid
  @NotNull
  private RabbitConfiguration rabbitConfiguration;

  @Valid
  @NotNull
  private List<QueueConfiguration> queues;
}
