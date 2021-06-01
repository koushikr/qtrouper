## qTrouper - Dropwizard bundle integration example 

This the is an example module on how one can integrate `qtrouper-dw` into their dropwizard application.

#### Maven Dependency
These lib's are hosted at clojars, so add below to your `repositories` in `pom`
```xml
<repository>
    <id>clojars</id>
    <name>Clojars repository</name>
    <url>https://clojars.org/repo</url>
</repository>
```
Add below to your `dependencies` in `pom`
```xml
<dependency>
    <groupId>io.github.qtrouper</groupId>
    <artifactId>qtrouper-dw</artifactId>
    <version>0.0.1-1</version>
</dependency>
```

#### Add bundle to application
Create a new `TrouperBundle` instance and there is one abstract method which client needs to implements to give rabbitMQ connection details and then add the bundle to `bootstrap` in `Application#initialize`. Here is a sample:
```java
  TrouperBundle<BasicConfiguration> trouper = new TrouperBundle<BasicConfiguration>() {
    @Override
    public RabbitConfiguration getRabbitConfiguration(BasicConfiguration configuration) {
      return configuration.getRabbitConfiguration();
    }
  };

  @Override
  public void initialize(Bootstrap<BasicConfiguration> bootstrap) {
    
    ...

    bootstrap.addBundle(trouper);

    ...
  }
```
Refer for `BasicApplication` for more details.

#### Create consumers
Create a new consumer extending `ManagedTrouper` and implement processors. Here is the sample:
```java
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
```
Refer `NotificationSender` and `Acknowledger` classes of this module for more details.
> Note: Please make sure that you annotate this class with `@Singleton` only then dropwizard would manage the lifecycle of this object.

### That's it!
Yeah, that's it, run your application your troupers would be ready to publish and consume.

