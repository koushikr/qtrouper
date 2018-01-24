package com.github.qtrouper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.qtrouper.core.healthchecks.TrouperHealthCheck;
import com.github.qtrouper.core.rabbit.RabbitConfiguration;
import com.github.qtrouper.core.rabbit.RabbitConnection;
import com.github.qtrouper.utils.SerDe;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.NoArgsConstructor;

/**
 * @author koushik
 */
@NoArgsConstructor
public abstract class TrouperBundle<T extends Configuration> implements ConfiguredBundle<T> {

    public abstract RabbitConfiguration getRabbitConfiguration(T configuration);

    @Override
    public void run(T configuration, Environment environment) throws Exception {
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        environment.getObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        environment.getObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        environment.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        environment.getObjectMapper().configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        environment.getObjectMapper().configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true);

        SerDe.init(environment.getObjectMapper());

        final RabbitConnection rabbitConnection = new RabbitConnection(getRabbitConfiguration(configuration));

        environment.lifecycle().manage(rabbitConnection);
        environment.healthChecks().register("qTrouper-health-check", new TrouperHealthCheck(rabbitConnection));
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {

    }
}