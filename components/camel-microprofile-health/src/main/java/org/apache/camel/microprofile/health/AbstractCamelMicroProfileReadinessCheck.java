package org.apache.camel.microprofile.health;

import java.util.Map;

import org.apache.camel.impl.health.AbstractHealthCheck;

public abstract class AbstractCamelMicroProfileReadinessCheck extends AbstractHealthCheck {

    public static final String HEALTH_GROUP_READINESS = "camel.health.readiness";

    protected AbstractCamelMicroProfileReadinessCheck(String id) {
        super(HEALTH_GROUP_READINESS, id);
    }

    protected AbstractCamelMicroProfileReadinessCheck(String id, Map<String, Object> meta) {
        super(HEALTH_GROUP_READINESS, id, meta);
    }
}
