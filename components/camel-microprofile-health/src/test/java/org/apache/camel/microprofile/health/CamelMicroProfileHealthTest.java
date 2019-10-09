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

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;

import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.health.AbstractHealthCheck;
import org.apache.camel.impl.health.ContextHealthCheck;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.Before;
import org.junit.Test;

public class CamelMicroProfileHealthTest extends CamelTestSupport {

    SmallRyeHealthReporter reporter;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        reporter = new SmallRyeHealthReporter();
    }

    @Test
    public void testCamelContextHealthCheck() {
        HealthCheckRegistry healthCheckRegistry = HealthCheckRegistry.get(context);

        ContextHealthCheck contextHealthCheck = new ContextHealthCheck();
        contextHealthCheck.setCamelContext(context);
        contextHealthCheck.getConfiguration().setEnabled(true);
        healthCheckRegistry.register(contextHealthCheck);

        CamelMicroProfileLivenessCheck livenessCheck = new CamelMicroProfileLivenessCheck();
        livenessCheck.setCamelContext(context);
        reporter.addHealthCheck(livenessCheck);

        CamelMicroProfileReadinessCheck readinessCheck = new CamelMicroProfileReadinessCheck();
        readinessCheck.setCamelContext(context);
        reporter.addHealthCheck(readinessCheck);

        SmallRyeHealth health = reporter.getHealth();

        JsonParser parser = Json.createParser(new StringReader(getHealthOutput(health)));
        assertTrue("Health check content is empty", parser.hasNext());

        parser.next();
        JsonObject healthObject = parser.getObject();

        assertEquals(HealthCheckResponse.State.UP.name(), healthObject.getString("status"));

        JsonArray checks = healthObject.getJsonArray("checks");
        assertEquals(2, checks.size());

        Consumer<JsonObject> contextAssertion = jsonObject -> {
            assertEquals(HealthCheckResponse.State.UP.name(), jsonObject.getString("context"));
        };
        assertHealthCheckOutput(checks.getJsonObject(0), contextAssertion);
        assertHealthCheckOutput(checks.getJsonObject(1), contextAssertion);
    }

    @Test
    public void testCamelMicroProfileLivenessCheck() {
        HealthCheckRegistry healthCheckRegistry = HealthCheckRegistry.get(context);

        healthCheckRegistry.register(createLivenessCheck("liveness-1", null, builder -> builder.up()));
        healthCheckRegistry.register(createLivenessCheck("liveness-2", null, builder -> builder.up()));
        healthCheckRegistry.register(createReadinessCheck("readiness-3", null, builder -> builder.up()));

        CamelMicroProfileLivenessCheck livenessCheck = new CamelMicroProfileLivenessCheck();
        livenessCheck.setCamelContext(context);
        reporter.addHealthCheck(livenessCheck);

        SmallRyeHealth health = reporter.getHealth();

        JsonParser parser = Json.createParser(new StringReader(getHealthOutput(health)));
        assertTrue("Health check content is empty", parser.hasNext());

        parser.next();
        JsonObject healthObject = parser.getObject();
        assertEquals(HealthCheckResponse.State.UP.name(), healthObject.getString("status"));

        JsonArray checks = healthObject.getJsonArray("checks");
        assertEquals(1, checks.size());

        JsonObject checksObject = checks.getJsonObject(0);
        assertHealthCheckOutput(checksObject, jsonObject -> {
            assertEquals(HealthCheckResponse.State.UP.name(), jsonObject.getString("liveness-1"));
            assertEquals(HealthCheckResponse.State.UP.name(), jsonObject.getString("liveness-2"));
        });
    }

    @Test
    public void testCamelMicroProfileReadinessCheck() {
        HealthCheckRegistry healthCheckRegistry = HealthCheckRegistry.get(context);

        healthCheckRegistry.register(createLivenessCheck("liveness-1", null, builder -> builder.up()));
        healthCheckRegistry.register(createReadinessCheck("readiness-1", null, builder -> builder.up()));
        healthCheckRegistry.register(createReadinessCheck("readiness-2", null, builder -> builder.up()));

        CamelMicroProfileReadinessCheck readinessCheck = new CamelMicroProfileReadinessCheck();
        readinessCheck.setCamelContext(context);
        reporter.addHealthCheck(readinessCheck);

        SmallRyeHealth health = reporter.getHealth();

        JsonParser parser = Json.createParser(new StringReader(getHealthOutput(health)));
        assertTrue("Health check content is empty", parser.hasNext());

        parser.next();
        JsonObject healthObject = parser.getObject();System.out.println(healthObject);
        assertEquals(HealthCheckResponse.State.UP.name(), healthObject.getString("status"));

        JsonArray checks = healthObject.getJsonArray("checks");
        assertEquals(1, checks.size());

        assertHealthCheckOutput(checks.getJsonObject(0), jsonObject -> {
            assertEquals(HealthCheckResponse.State.UP.name(), jsonObject.getString("readiness-1"));
            assertEquals(HealthCheckResponse.State.UP.name(), jsonObject.getString("readiness-2"));
        });
    }

    @Test
    public void testCamelMicroProfileCombinedHealthChecks() {

    }

    @Test
    public void testCamelMicroProfileHealthMetaData() {
        HealthCheckRegistry healthCheckRegistry = HealthCheckRegistry.get(context);

        ContextHealthCheck contextHealthCheck = new ContextHealthCheck();
        contextHealthCheck.setCamelContext(context);
        contextHealthCheck.getConfiguration().setEnabled(true);
        healthCheckRegistry.register(contextHealthCheck);

        SmallRyeHealth health = reporter.getHealth();

        JsonParser parser = Json.createParser(new StringReader(getHealthOutput(health)));
        assertTrue("Health check content is empty", parser.hasNext());

        parser.next();
        JsonObject healthObject = parser.getObject();

        assertEquals(HealthCheckResponse.State.UP.name(), healthObject.getString("status"));

        JsonArray checks = healthObject.getJsonArray("checks");
        assertEquals(2, checks.size());

        Consumer<JsonObject> contextAssertion = jsonObject -> {
            assertEquals(HealthCheckResponse.State.UP.name(), jsonObject.getString("context"));
        };
        assertHealthCheckOutput(checks.getJsonObject(0), contextAssertion);
        assertHealthCheckOutput(checks.getJsonObject(1), contextAssertion);
    }

    private void assertHealthCheckOutput(JsonObject healthObject, Consumer<JsonObject> dataObjectAssertions) {
        assertEquals("camel", healthObject.getString("name"));
        assertEquals(HealthCheckResponse.State.UP.name(), healthObject.getString("status"));
        dataObjectAssertions.accept(healthObject.getJsonObject("data"));
    }

    private String getHealthOutput(SmallRyeHealth health) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        reporter.reportHealth(outputStream, health);
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private HealthCheck createLivenessCheck(String id, Map<String, Object> options, Consumer<HealthCheckResultBuilder> consumer) {
        HealthCheck healthCheck = new AbstractCamelMicroProfileLivenessCheck(id, options) {
            @Override
            protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
                consumer.accept(builder);
            }
        };
        healthCheck.getConfiguration().setEnabled(true);
        return healthCheck;
    }

    private HealthCheck createReadinessCheck(String id, Map<String, Object> options, Consumer<HealthCheckResultBuilder> consumer) {
        HealthCheck healthCheck = new AbstractCamelMicroProfileReadinessCheck(id, options) {
            @Override
            protected void doCall(HealthCheckResultBuilder builder, Map<String, Object> options) {
                consumer.accept(builder);
            }
        };
        healthCheck.getConfiguration().setEnabled(true);
        return healthCheck;
    }
}
