package org.apache.camel.microprofile.health;

import java.util.Map;

import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;

public class DummyHealthCheck extends AbstractHealthCheck {

    protected DummyHealthCheck() {
        super("dummy", "dummy");
        getConfiguration().setEnabled(true);
    }

    @Override
    protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
        builder.up();
    }
}
