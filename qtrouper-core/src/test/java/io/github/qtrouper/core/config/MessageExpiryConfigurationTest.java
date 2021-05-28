package io.github.qtrouper.core.config;

import static io.github.qtrouper.core.config.MessageExpiryConfiguration.MessageExpiryAction.IGNORE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MessageExpiryConfigurationTest {

  @Test
  public void testWhenMessageExpiryConfigurationDefaultViaConstructor() {

    MessageExpiryConfiguration messageExpiryConfiguration = new MessageExpiryConfiguration();

    Assertions.assertFalse(messageExpiryConfiguration.isEnabled());
    Assertions.assertEquals(0L, messageExpiryConfiguration.getThresholdInMs());
    Assertions.assertEquals(IGNORE, messageExpiryConfiguration.getAction());
  }

  @Test
  public void testWhenMessageExpiryConfigurationDefaultViaBuilder() {

    MessageExpiryConfiguration messageExpiryConfiguration = MessageExpiryConfiguration.builder().build();

    Assertions.assertFalse(messageExpiryConfiguration.isEnabled());
    Assertions.assertEquals(0L, messageExpiryConfiguration.getThresholdInMs());
    Assertions.assertEquals(IGNORE, messageExpiryConfiguration.getAction());
  }

}