package org.apache.camel.microprofile.health;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

@ApplicationScoped
public class CamelContextProducer {

    @Produces
    @Singleton
    public CamelContext createCamelContext() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .throwException(new RuntimeException("Billabong!"));
            }
        });
        camelContext.getRegistry().bind("dummy", new DummyHealthCheck());

        return camelContext;
    }
}
