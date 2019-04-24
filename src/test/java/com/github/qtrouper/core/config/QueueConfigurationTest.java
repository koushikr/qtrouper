package com.github.qtrouper.core.config;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class QueueConfigurationTest {

  @Test
  public void testQueueConfigurationDefaultViaConstructor() {


    QueueConfiguration queueConfiguration = new QueueConfiguration();

    Assert.assertFalse(queueConfiguration.isConsumerDisabled());
    Assert.assertEquals(queueConfiguration.getConcurrency(), 3);
    Assert.assertEquals(queueConfiguration.getNamespace(), "qtrouper");


  }

  @Test
  public void testQueueConfigurationDefaultViaBuilder() {

    QueueConfiguration queueViaBuilder = QueueConfiguration.builder().build();

    Assert.assertFalse(queueViaBuilder.isConsumerDisabled());
    Assert.assertEquals(queueViaBuilder.getConcurrency(), 3);
    Assert.assertEquals(queueViaBuilder.getNamespace(), "qtrouper");

  }

}