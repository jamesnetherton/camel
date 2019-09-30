package org.apache.camel.microprofile.health;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.camel.CamelContext;
import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.health.AbstractHealthCheck;

@ApplicationScoped
public class CamelContextProducer {

    @Produces
    @Singleton
    public CamelContext createCamelContext() {
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.getRegistry().bind("dummy", new DummyHealthCheck("dummy"));

        return camelContext;
    }

    private class DummyHealthCheck extends AbstractHealthCheck {

        protected DummyHealthCheck(String id) {
            super(id);
        }

        @Override
        protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
            builder.down();
        }
    }
}
