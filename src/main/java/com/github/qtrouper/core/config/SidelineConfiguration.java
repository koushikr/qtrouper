package com.github.qtrouper.core.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * @author koushik
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class SidelineConfiguration {

    private boolean enabled;

    private int concurrency;

    public static SidelineConfiguration getDefaultConfiguration() {
        return SidelineConfiguration.builder()
                .enabled(true)
                .concurrency(0)
                .build();
    }
}
