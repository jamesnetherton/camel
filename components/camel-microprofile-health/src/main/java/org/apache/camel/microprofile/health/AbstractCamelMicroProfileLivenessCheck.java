package org.apache.camel.microprofile.health;

import java.util.Map;

import org.apache.camel.impl.health.AbstractHealthCheck;

public abstract class AbstractCamelMicroProfileLivenessCheck extends AbstractHealthCheck {

    public static final String HEALTH_GROUP_LIVENESS = "camel.health.liveness";

    protected AbstractCamelMicroProfileLivenessCheck(String id) {
        super(HEALTH_GROUP_LIVENESS, id);
    }

    protected AbstractCamelMicroProfileLivenessCheck(String id, Map<String, Object> meta) {
        super(HEALTH_GROUP_LIVENESS, id, meta);
    }
}
