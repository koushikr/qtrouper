package com.github.qtrouper.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @author koushik
 */
@Data
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueueConfiguration {

    private static final String DEFAULT_NAMESPACE = "qtrouper";

    private String namespace = DEFAULT_NAMESPACE;

    private String queueName;

    @Min(1)
    @Max(100)
    private int concurrency = 3;

    @Min(1)
    @Max(100)
    private int prefetchCount = 1;

    private RetryConfiguration retry;

    private SidelineConfiguration sideline;

    public static QueueConfiguration getDefaultConfiguration(String queueName) {
        return QueueConfiguration.builder()
                .namespace(DEFAULT_NAMESPACE)
                .queueName(queueName)
                .prefetchCount(1)
                .concurrency(3)
                .retry(RetryConfiguration.getDefaultRetryConfiguration())
                .sideline(SidelineConfiguration.getDefaultConfiguration())
                .build();
    }


}
