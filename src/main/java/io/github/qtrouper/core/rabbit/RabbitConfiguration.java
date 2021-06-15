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
package io.github.qtrouper.core.rabbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

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

    private boolean sslEnabled;

    private boolean metricsEnabled;

}
