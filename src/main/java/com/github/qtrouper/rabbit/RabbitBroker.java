package com.github.qtrouper.rabbit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @author koushik
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RabbitBroker {

    @NotEmpty
    @NotNull
    private String host;

    @Min(0)
    @Max(65535)
    private int port;

}
