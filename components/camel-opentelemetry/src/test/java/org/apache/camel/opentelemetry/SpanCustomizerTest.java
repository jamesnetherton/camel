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
package org.apache.camel.opentelemetry;

import java.util.Arrays;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.junit.jupiter.api.Test;

public class SpanCustomizerTest extends CamelOpenTelemetryTestSupport {
    private static final SpanTestData[] TEST_DATA = {
            new SpanTestData().setOperation("external-parent"),
            new SpanTestData().setUri("seda://next").setOperation("next")
                    .setParentId(2),
            new SpanTestData().setUri("seda://next").setOperation("next")
                    .setKind(SpanKind.CLIENT)
                    .setParentId(3),
            new SpanTestData().setUri("direct://start").setOperation("start")
                    .setParentId(0),
    };
    private String traceId;

    SpanCustomizerTest() {
        super(TEST_DATA);
    }

    @Test
    public void testCustomize() {
        template.requestBody("direct:start", traceId);
        verify();
    }

    @Override
    protected void initTracer(CamelContext context) {
        context.getRegistry().bind("spanCustomizer", spanCustomizer());
        super.initTracer(context);

        Span span = tracer.spanBuilder("external-parent").setAttribute("component", "foo").startSpan();
        traceId = span.getSpanContext().getTraceId();
        span.end();

        Arrays.stream(TEST_DATA).forEach(td -> td.setTraceId(traceId));
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").routeId("start")
                        .to("seda:next");

                from("seda:next")
                        .log("${body}");
            }
        };
    }

    private SpanCustomizer spanCustomizer() {
        return new SpanCustomizer() {
            @Override
            public void customize(SpanBuilder spanBuilder, String operationName, Exchange exchange) {
                if (operationName.equals("start") && exchange.getFromRouteId().equals("start")) {
                    // Use a custom trace id for propagation to all spans generated from direct:start routing
                    String traceId = exchange.getMessage().getBody(String.class);
                    SpanContext spanContext = SpanContext.create(traceId,
                            IdGenerator.random().generateSpanId(),
                            TraceFlags.getSampled(),
                            TraceState.getDefault());

                    spanBuilder.setParent(Context.root().with(Span.wrap(spanContext)));
                }
            }
        };
    }
}
