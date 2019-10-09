/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.microprofile.health;

import java.util.Collection;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.health.HealthCheck.Result;
import org.apache.camel.health.HealthCheck.State;
import org.apache.camel.health.HealthCheckFilter;
import org.apache.camel.health.HealthCheckHelper;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

public abstract class AbstractCamelMicroProfileHealthCheck implements HealthCheck, CamelContextAware {

    @Inject
    protected CamelContext camelContext;

    @Override
    public HealthCheckResponse call() {
        final HealthCheckResponseBuilder builder = HealthCheckResponse.builder();
        builder.name("camel");

        if (camelContext != null) {
            Collection<Result> results = HealthCheckHelper.invoke(camelContext, (HealthCheckFilter) check -> check.getGroup().equals(getHealthGroupFilterExclude()));
            if (!results.isEmpty()) {
                builder.up();
            }

            for (Result result: results) {
                builder.withData(result.getCheck().getId(), result.getState().name());
                result.getDetails().forEach((k, v) -> {
                    if (v instanceof Long) {
                        builder.withData(k, (Long) v);
                    } else if (v instanceof String) {
                        builder.withData(k, (String) v);
                    } else if (v instanceof Boolean) {
                        builder.withData(k, (Boolean) v);
                    } else {
                        builder.withData(k, v.toString());
                    }
                });

                if (result.getState() == State.DOWN) {
                    builder.down();
                }
            }
        }

        return builder.build();
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    abstract String getHealthGroupFilterExclude();
}
