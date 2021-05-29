package io.github.qtrouper.dw.example;

import io.dropwizard.Application;
import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.github.qtrouper.TrouperBundle;
import io.github.qtrouper.core.rabbit.RabbitConfiguration;
import io.github.qtrouper.dw.example.config.BasicConfiguration;
import io.github.qtrouper.dw.example.modules.BasicModule;
import ru.vyarus.dropwizard.guice.GuiceBundle;

public class BasicApplication extends Application<BasicConfiguration> {

  TrouperBundle<BasicConfiguration> trouper = new TrouperBundle<BasicConfiguration>() {
    @Override
    public RabbitConfiguration getRabbitConfiguration(BasicConfiguration configuration) {
      return configuration.getRabbitConfiguration();
    }
  };

  public static void main(final String[] args) throws Exception {
    new BasicApplication().run("server", "basic_config.yml");
  }

  @Override
  public void run(BasicConfiguration configuration, Environment environment) {
  }

  @Override
  public void initialize(Bootstrap<BasicConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(new ResourceConfigurationSourceProvider());

    bootstrap.addBundle(trouper);

    bootstrap.addBundle(GuiceBundle.<BasicConfiguration>builder()
        .enableAutoConfig("io.github.qtrouper.dw.example")
        .modules(new BasicModule<BasicConfiguration>(trouper))
        .build());
  }
}
