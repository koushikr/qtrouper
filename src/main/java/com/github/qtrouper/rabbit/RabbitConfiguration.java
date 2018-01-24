package com.github.qtrouper.rabbit;

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
class RabbitConfiguration {

    @NotNull
    @NotEmpty
    private List<RabbitBroker> brokers;

    private int threadPoolSize = 128;

    @NotNull
    private String userName = "";

    @NotNull
    private String password = "";

    private String virtualHost;
}
