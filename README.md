# QTrouper

[![Travis build status](https://travis-ci.org/koushikr/qtrouper.svg?branch=master)](https://travis-ci.org/koushikr/qtrouper)

The repository url for the same
```
    <repository>
        <id>qtrouper</id>
        <url>https://raw.github.com/koushikr/qtrouper/mvn-repo/</url>
        <snapshots>
            <enabled>true</enabled>
            <updatePolicy>always</updatePolicy>
        </snapshots>
    </repository>
```

You could find the dependency at

```
    <dependency>
            <groupId>com.github.koushikr</groupId>
            <artifactId>qtrouper</artifactId>
            <version>0.0.1</version>
    </dependency>
```


QTrouper provides the following capabilities
  - Create a hierarchy of retry and sideline for queue
  - Configure the number of times and the backoff on the retry queue
  - Enable consumption on the sideline queue or otherwise.
  - Queue the entire context with intermediate objects, so long as they are serializable.
  - An interface for consumption of main queue and sideline queue messages

# Key Concepts

  - Main Queue      : A queue on which the primary action is performed. Define a consumer on this queue by extending the Trouper class and implementing its process method.
  - Retry Queue     : A retry queue is not visible to the user. It is responsible for expiring a message and dead lettering into main queue till the maxRetryCount is not breached.
  - Sideline Queue  : A sideline queue on which the tertiary action is performed. Define a consumer on this queue by extending the Trouper class and implementing its processSideline method
  - QueueContex     : A Map to a key + a serializable object that gets enqueued in the queues
  - Trouper         : The Trouper is the queueing interface to provide main_queue, retry_queue and sideline_queue implementations for any call to action.


> The overriding design goal for qtrouper model
> is to make it as ready as possible to onboard a new message processor.
> The idea is that a formatted consumer definition is
> usable as-is, as a processing unit inside the app, without
> having to create any additiional boiler plate code
> to support the same. And a dropwizard bundle is made out of it
> for easy integration with dropwizard apps.

### Tech

QTrouper uses rabbitMQ as its backend interface and the

* [Dropwizard](https://github.com/dropwizard/dropwizard) - The bundle that got created
* [RabbitMQ](https://www.rabbitmq.com/) - Messaging that just works.

### Example

## Sample Configuration

```
static class SampleConfiguration extends Configuration{

        private RabbitConfiguration rabbitConfiguration;

    }
}

```

## Bundle Inclusion

```
      TrouperBundle<SampleConfiguration> trouperBundle = new TrouperBundle<SampleConfiguration>() {

                @Override
                public RabbitConfiguration getRabbitConfiguration(SampleConfiguration configuration) {
                    return null;
                }
      };

      bootstrap.addBundle(trouperBundle);

```

## Sample Actor

```

static class QueueingActor extends QTrouper<QueueContext> {

        @Inject
        public SymphonyActor(QueueConfiguration consumerConfiguration, RabbitConnection rabbitConnection) {
            super("sampleQueue",
                    consumerConfiguration,
                    rabbitConnection,
                    QueueContext.class,
                    Collections.emptySet());
        }

        @Override
        protected boolean process(QueueContext queueContext, QAccessInfo accessInfo){
            try{
                return processor.consume(queueContext, accessInfo);
            }catch (Exception e){
                log.error("Error processing a main queue message for reference Id {}", queueContext.getServiceReference());
                return false;
            }
        }

        @Override
        protected boolean processSideline(QueueContext queueContext, QAccessInfo accessInfo) {
            try{
                return processor.consumeSideline(queueContext, accessInfo);
            }catch (Exception e){
                log.error("Error processing a sideline queue message for reference Id {}", queueContext.getServiceReference());
                return false;
            }
        }
    }

```


