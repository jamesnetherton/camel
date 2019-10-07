package org.apache.camel.microprofile.health;

import io.smallrye.health.SmallRyeHealth;
import io.smallrye.health.SmallRyeHealthReporter;

import javax.inject.Inject;

import org.apache.camel.BindToRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.health.HealthCheckRegistry;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.health.RoutePerformanceCounterEvaluators;
import org.apache.camel.impl.health.RoutesHealthCheckRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;

//@RunWith(Arquillian.class)
public class TestHealthCheck {

    @BindToRegistry("dummyHealthCheck")
    protected DummyHealthCheck dummyHealthCheck = new DummyHealthCheck();

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
    public void testHealth() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .throwException(new Exception());
            }
        });
        camelContext.getRegistry().bind("dummy", dummyHealthCheck);
        CamelMicroProfileHealthCheck check = new CamelMicroProfileHealthCheck();
        check.setCamelContext(camelContext);

        SmallRyeHealthReporter reporter = new SmallRyeHealthReporter();
        reporter.addHealthCheck(check);

        HealthCheckRegistry healthCheckRegistry = HealthCheckRegistry.get(camelContext);

        RoutesHealthCheckRepository repository = new RoutesHealthCheckRepository();
        healthCheckRegistry.addRepository(repository);
        repository.addEvaluator(new RoutePerformanceCounterEvaluators.ExchangesFailed(1));
//        repository.addEvaluator(new RoutePerformanceCounterEvaluators.LastProcessingTime(1000, 1));

        camelContext.start();
        try {
            ProducerTemplate template = camelContext.createProducerTemplate();

            for (int i = 0; i < 2; i++) {
                try {
                    template.sendBody("direct:start", null);
                } catch (Throwable t) {
                }
            }

            SmallRyeHealth health = reporter.getHealth();
            reporter.reportHealth(System.out, health);
        } finally {
            camelContext.stop();
        }
    }
}
