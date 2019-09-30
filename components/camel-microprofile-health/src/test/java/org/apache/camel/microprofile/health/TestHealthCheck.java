package org.apache.camel.microprofile.health;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

import java.util.Map;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.camel.BindToRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.health.HealthCheckResultBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.health.AbstractHealthCheck;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class TestHealthCheck {

    @BindToRegistry("dummyHealthCheck")
    protected DummyHealthCheck dummyHealthCheck = new DummyHealthCheck("dummy");

    @Inject
    SmallRyeHealthReporter reporter;

    @Inject
    CamelContext camelContext;

    @Deployment
    public static Archive<?> deployment() {
        return ShrinkWrap.create(JavaArchive.class)
                // Test class
                .addClass(TestHealthCheck.class)
                .addClass(CamelMicroProfileHealthCheck.class)
                .addClass(CamelContextProducer.class)
                .addPackage("io.smallrye.health")
//                .addPackage("io.smallrye.config")
                .addPackage("io.smallrye.config")

                // Bean archive deployment descriptor
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testHealth() {
//        CamelContext camelContext = new DefaultCamelContext();
        //camelContext.getRegistry().bind("dummy", dummyHealthCheck);
//        reporter.addHealthCheck(new CamelHealthCheck());

        camelContext.start();
        try {
            SmallRyeHealth health = reporter.getHealth();
            reporter.reportHealth(System.out, health);
        } finally {
            camelContext.stop();
        }
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
