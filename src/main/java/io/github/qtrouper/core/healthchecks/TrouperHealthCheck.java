/*
 * Copyright 2019 Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.qtrouper.core.healthchecks;

import com.codahale.metrics.health.HealthCheck;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.github.qtrouper.core.rabbit.RabbitConnection;
import lombok.AllArgsConstructor;
import lombok.val;

import javax.inject.Singleton;
import java.io.IOException;

/**
 * @author koushik
 */
@Singleton
@AllArgsConstructor
public class TrouperHealthCheck extends HealthCheck{
    private final RabbitConnection rabbitConnection;

    @Override
    protected Result check() throws Exception {
        val connection = rabbitConnection.getConnection();
        Result result;
        if (connection != null && connection.isOpen()) {
            try {
                val channel = connection.createChannel();
                channel.close();
                result = Result.healthy();
            } catch (IOException e) {
                result = Result.unhealthy("Connection is open, could not create channel");
            }
        } else {
            result = Result.unhealthy("Not Connected");
        }
        return result;
    }
}
