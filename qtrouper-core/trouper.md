# All about [Trouper](https://github.com/koushikr/qtrouper/blob/master/qtrouper-core/src/main/java/io/github/qtrouper/Trouper.java)
> A reliable and uncomplaining actor, give all the messages you have and trouper is happy to consume

## Basics
- `Trouper` is a abstract class which acts as a processor for one queue, handling publishing, consuming, retrying, sidelining
- Each trouper needs a `QueueConfiguration` to understand your needs on queue functionality and message processing. Know more about `QueueConfiguration` [here](https://todo.next)
- Each trouper would also need some basic details like queueName, messageJavaType, knownExceptionTypes required for processing.
- For each trouper all you have to do is implement a method, how you would like to process the message, that's it!

So a trouper would like something like this:
```java
public class NotificationSender extends Trouper<NotificationMessage> {

    private final NotificationService notificationService;

    public NotificationSender() {
        super("NOTIFICATION_SENDER_QUEUE", // queueName
            queueConfiguration, // required queue config
            rabbitConnection, // RMQ connection
            NotificationMessage.class, // messageClassType
            Collections.emptySet()); // known exceptions set
        notificationService = new NotificationService();
    }

    @Override
    public boolean process(NotificationMessage notificationMessage, QAccessInfo accessInfo) {
        notificationService.notify(notificationMessage);
        return true;
    }

    @Override
    public boolean processSideline(NotificationMessage notificationMessage, QAccessInfo accessInfo) {
        process(notificationMessage, accessInfo);
        return true;
    }
}
```

## Understanding interface
We can break the complete trouper interface into three areas
- Init's
- Publish
- Consume

### Init's
```java
void start() // Required to be called before using any of trouper functionality
```
```java
void stop() // Required to be called before shutting down the application
```

### Publish
Multiple options on how we can publish a message
```java
void publish(Message message) 
// Publishes the message to mainQueue in persistent mode
```
```java
void publishWithExpiry(Message message, long expiresAt, boolean expiresAtEnabled) 
// Publishes the message which gets expired at given timestamp if expiration is enabled
```
```java
void sidelinePublish(Message message) 
//  Publishes the message to sideline queue
```
```java
void retryPublish(Message message, int retryCount, long expiration) 
// Publishes the message to retry queue with retryCount and expiration which would further deadLetter into the mainQueue.
```
```java
void retryPublishWithExpiry(Message message, int retryCount, long expiration, long expiresAt, boolean expiresAtEnabled) 
// Publishes the message to retry queue with retryCount and expiration and expiryTimestamp which would further deadLetter into the mainQueue
```

### Consume
Below consume methods are abstract and needs to implemented by the concrete class
```java
abstract boolean process(Message message, QAccessInfo accessInfo) 
// Process message on main queue and return's true if successfully processed
```
```java
abstract boolean processSideline(Message message, QAccessInfo accessInfo) 
// Process message on sideline queue and return's true if successfully processed
```