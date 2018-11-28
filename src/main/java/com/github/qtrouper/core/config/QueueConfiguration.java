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

    @Builder.Default
    private String namespace = DEFAULT_NAMESPACE;

    private String queueName;

    @Min(1)
    @Max(100)
    @Builder.Default
    private int concurrency = 3;

    @Min(1)
    @Max(100)
    @Builder.Default
    private int prefetchCount = 1;

    private boolean consumerAvailable;

    private RetryConfiguration retry;

    private SidelineConfiguration sideline;

}
