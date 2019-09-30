package org.apache.camel.microprofile.health;

import java.util.Collection;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.health.HealthCheck.Result;
import org.apache.camel.health.HealthCheck.State;
import org.apache.camel.health.HealthCheckHelper;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

@Liveness
@Readiness
public class CamelMicroProfileHealthCheck implements HealthCheck, CamelContextAware {

    @Inject
    private CamelContext camelContext;

    @Override
    public HealthCheckResponse call() {
        final HealthCheckResponseBuilder builder = HealthCheckResponse.builder();
        builder.name("camel-health");

        if (camelContext != null) {
            Collection<Result> results = HealthCheckHelper.invoke(camelContext);
            if (!results.isEmpty()) {
                builder.up();
            }

            for (Result result: results) {
                builder.withData(result.getCheck().getId(), result.getState().name());
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
}
