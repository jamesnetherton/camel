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
package org.apache.camel.language.jq;

import net.thisptr.jackson.jq.v2.spi.Function;
import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

public class JqRegistryFunctionTest extends JqTestSupport {

    @Test
    public void testFunctionFromRegistry() throws Exception {
        getMockEndpoint("mock:result").expectedBodiesReceived(MAPPER.createObjectNode().put("foo", "beer"));

        template.sendBody("direct:containsBeer", MAPPER.createObjectNode().put("foo", "baz"));
        template.sendBody("direct:containsBeer", MAPPER.createObjectNode().put("foo", "beer"));

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    public void testFunctionWithArityFromRegistry() throws Exception {
        getMockEndpoint("mock:result").expectedBodiesReceived(MAPPER.createObjectNode().put("foo", "wine"));

        template.sendBody("direct:containsWine", MAPPER.createObjectNode().put("foo", "baz"));
        template.sendBody("direct:containsWine", MAPPER.createObjectNode().put("foo", "wine"));

        MockEndpoint.assertIsSatisfied(context);
    }

    @BindToRegistry
    public Function containsBeer() {
        return new JqCustomScopeTest.ContainsFunction("beer");
    }

    @BindToRegistry("containsWine/1")
    public Function containsWine() {
        return new JqCustomScopeTest.ContainsFunction("wine");
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:containsBeer")
                        .filter().jq("containsBeer(.foo)")
                        .to("mock:result");

                from("direct:containsWine")
                        .filter().jq("containsWine(.foo)")
                        .to("mock:result");
            }
        };
    }
}
