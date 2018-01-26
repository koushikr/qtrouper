package com.github.qtrouper.core.rabbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author koushik
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RabbitConfiguration {

    @NotNull
    @NotEmpty
    private List<RabbitBroker> brokers;

    @Builder.Default
    private int threadPoolSize = 128;

    @NotNull
    @Builder.Default
    private String userName = "";

    @NotNull
    @Builder.Default
    private String password = "";

    private String virtualHost;
}
