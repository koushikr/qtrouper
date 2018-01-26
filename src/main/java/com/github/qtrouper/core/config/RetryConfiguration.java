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

    @Builder.Default
    private boolean enabled = true;

    private long ttlMs;

    private int maxRetries;

    private int backOffFactor; //multiplicationFactor

}
