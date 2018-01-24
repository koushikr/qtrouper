package com.github.qtrouper.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

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
public class RetryConfiguration {

    private boolean enabled;

    private long ttlMs;

    private int maxRetries;

    private int backOffFactor; //multiplicationFactor

    public static RetryConfiguration getDefaultRetryConfiguration() {
        return RetryConfiguration.builder()
                .enabled(true)
                .ttlMs(1000)
                .maxRetries(3)
                .backOffFactor(2)
                .build();
    }
}
