package org.apache.camel.microprofile.health;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.health.ContextHealthCheck;
import org.apache.camel.impl.health.RoutePerformanceCounterEvaluators;
import org.apache.camel.impl.health.RoutesHealthCheckRepository;
import org.apache.camel.spi.Registry;
import org.junit.Test;

public class TestHealthCheck {

    @Test
    public void testHealthChecks() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();

        HealthCheckRegistry healthCheckRegistry = HealthCheckRegistry.get(camelContext);

        ContextHealthCheck contextHealthCheck = new ContextHealthCheck();
        contextHealthCheck.setCamelContext(camelContext);
        contextHealthCheck.getConfiguration().setEnabled(true);
//        healthCheckRegistry.register(contextHealthCheck);

        CustomHealthCheck customHealthCheck = new CustomHealthCheck();
        customHealthCheck.getConfiguration().setEnabled(true);
        healthCheckRegistry.register(customHealthCheck);

        RoutesHealthCheckRepository repository = new RoutesHealthCheckRepository();
//        healthCheckRegistry.addRepository(repository);

        SmallRyeHealthReporter reporter = new SmallRyeHealthReporter();
        CamelMicroProfileHealthCheck microProfileHealthCheck = new CamelMicroProfileHealthCheck();
        microProfileHealthCheck.setCamelContext(camelContext);
        reporter.addHealthCheck(microProfileHealthCheck);

        camelContext.start();
        try {
//            camelContext.getRouteController().stopRoute("route1");
            SmallRyeHealth health = reporter.getHealth();
            reporter.reportHealth(System.out, health);
        } finally {
            camelContext.stop();
        }
    }
}
