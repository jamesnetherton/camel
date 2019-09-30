package org.apache.camel.microprofile.health;

import java.util.stream.Stream;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckRepository;

public class CamelMicroProfileHealthRepository implements HealthCheckRepository, CamelContextAware {

    @Override
    public void setCamelContext(CamelContext camelContext) {

    }

    @Override
    public CamelContext getCamelContext() {
        return null;
    }

    @Override
    public Stream<HealthCheck> stream() {
        return null;
    }
}
