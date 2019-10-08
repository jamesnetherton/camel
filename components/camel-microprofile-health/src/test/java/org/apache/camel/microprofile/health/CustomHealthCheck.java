package org.apache.camel.microprofile.health;

import java.util.Map;

import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;

public class CustomHealthCheck extends AbstractHealthCheck {

    protected CustomHealthCheck() {
        super("test", "test");
    }

    @Override
    protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
//        builder.detail("foo", "bar");
//        builder.up();
    }
}
