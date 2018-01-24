package com.github.qtrouper.core.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author koushik
 */
@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrouperConfiguration {

    @NotNull @NotEmpty
    private String handlerPackage;

    private List<QueueConfiguration> queues = new ArrayList<>();

    @JsonIgnore
    public QueueConfiguration getConsumerConfiguration(String queueName) {
        if (queues == null || queues.isEmpty())
            return QueueConfiguration.getDefaultConfiguration(queueName);

        final Optional<QueueConfiguration> first = queues.stream().filter(each -> each.getQueueName().equalsIgnoreCase(queueName)).findFirst();

        return first.orElseGet(() -> QueueConfiguration.getDefaultConfiguration(queueName));
    }

}
