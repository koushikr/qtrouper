package com.github.qtrouper.core;

import com.github.qtrouper.core.models.QAccessInfo;
import com.github.qtrouper.core.models.QueueContext;
import com.github.qtrouper.rabbit.RabbitConnection;
import com.github.qtrouper.trouper.QueueConfiguration;
import com.github.qtrouper.trouper.TrouperConfiguration;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author koushik
 */
@Slf4j
@Singleton
public class QueueManager {

    private static Map<String, TrouperActor> actors = new HashMap<>();
    private final RabbitConnection rabbitConnection;
    private final TrouperConfiguration trouperConfiguration;
    private Map<String, QueueConsumer> qProcessors = new HashMap<>();

    public QueueManager(RabbitConnection rabbitConnection, TrouperConfiguration configuration) {
        this.rabbitConnection = rabbitConnection;
        this.trouperConfiguration = configuration;
    }

    private static TrouperActor getActor(String queueName) {
        if (!actors.containsKey(queueName))
            throw new RuntimeException("Can't find a queue actor with the queuename : " + queueName);

        return actors.get(queueName);
    }

    private QueueConsumer getProcessor(String queueName) {
        if (!qProcessors.containsKey(queueName))
            throw new RuntimeException("Can't find a queue processor with the queuename : " + queueName);

        return qProcessors.get(queueName);
    }

    public void start() throws Exception {
        log.info("Starting the Q registrar");

        for (String queueName : qProcessors.keySet()) {

            TrouperActor trouperActor = TrouperActor.builder()
                    .queueName(queueName)
                    .consumerConfiguration(
                            trouperConfiguration.getConsumerConfiguration(queueName)
                    )
                    .rabbitConnection(rabbitConnection)
                    .processor(getProcessor(queueName))
                    .build();

            trouperActor.start();

            actors.put(queueName, trouperActor);
        }

        log.info("Started all symphony queues");
    }

    public void stop() throws Exception {
        log.info("Stopping all symphony actors");
        actors.values().stream().forEach(QTrouper::stop);
        log.info("Stopped all symphony actors");
    }

    public Optional<Boolean> publish(String queueName, QueueContext queueContext) throws Exception {
        TrouperActor actor = getActor(queueName);

        return Optional.of(actor.publish(queueContext));
    }

    public Optional<Boolean> ttlPublish(String queueName, QueueContext queueContext, int retryCount, long expirationMs) throws Exception {
        TrouperActor actor = getActor(queueName);

        return Optional.of(actor.retryPublish(queueContext, retryCount, expirationMs));
    }

    @Slf4j
    static class TrouperActor extends QTrouper<QueueContext> {

        private final QueueConsumer processor;

        @Builder
        public TrouperActor(String queueName,
                            QueueConfiguration consumerConfiguration,
                            RabbitConnection rabbitConnection,
                            QueueConsumer processor) {
            super(queueName,
                    consumerConfiguration,
                    rabbitConnection,
                    QueueContext.class,
                    Collections.emptySet());

            this.processor = processor;
        }

        @Override
        protected boolean process(QueueContext queueContext, QAccessInfo accessInfo){
            try{
                return processor.consume(queueContext, accessInfo);
            }catch (Exception e){
                log.error("Error processing a main queue message for reference Id {}", queueContext.getReferenceId());
                return false;
            }
        }

        @Override
        protected boolean processSideline(QueueContext queueContext, QAccessInfo accessInfo) {
            try{
                return processor.consumeSideline(queueContext, accessInfo);
            }catch (Exception e){
                log.error("Error processing a sideline queue message for reference Id {}", queueContext.getReferenceId());
                return false;
            }
        }
    }
}
