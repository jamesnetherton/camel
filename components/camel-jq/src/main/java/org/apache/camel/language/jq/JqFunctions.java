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

import java.util.List;
import java.util.Map;

import net.thisptr.jackson.jq.v2.core.EnvironmentBuilder;
import net.thisptr.jackson.jq.v2.core.version.Versions;
import net.thisptr.jackson.jq.v2.json.JsonNodeType;
import net.thisptr.jackson.jq.v2.json.JsonProvider;
import net.thisptr.jackson.jq.v2.json.impl.jackson3.Jackson3JsonProvider;
import net.thisptr.jackson.jq.v2.spi.BindContext;
import net.thisptr.jackson.jq.v2.spi.Expression;
import net.thisptr.jackson.jq.v2.spi.Function;
import net.thisptr.jackson.jq.v2.spi.FunctionSignature;
import net.thisptr.jackson.jq.v2.spi.Output;
import net.thisptr.jackson.jq.v2.spi.RuntimeContext;
import net.thisptr.jackson.jq.v2.spi.annotations.FunctionRegistration;
import net.thisptr.jackson.jq.v2.spi.exception.JsonQueryException;
import net.thisptr.jackson.jq.v2.spi.path.Path;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.StreamCache;
import org.apache.camel.util.concurrent.ContextValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

/**
 * The Camel supplied jq functions.
 * <p/>
 * The functions are loaded by the jackson-jq {@code ServiceLoader} lookup, so a new function must be annotated with
 * {@link FunctionRegistration} and listed in {@code META-INF/services/net.thisptr.jackson.jq.v2.spi.Function},
 * otherwise it is not available in jq expressions.
 */
public final class JqFunctions {

    public static final ContextValue<Exchange> EXCHANGE_LOCAL = ContextValue.newThreadLocal("JqExchange");

    private static final Logger LOGGER = LoggerFactory.getLogger(JqFunctions.class);

    private JqFunctions() {
    }

    /**
     * Creates an environment builder with the jq builtins and any functions and modules found on the classpath, which
     * includes the Camel functions in this class as they are registered as {@link Function} services.
     */
    public static EnvironmentBuilder<JsonNode> newEnvironmentBuilder(CamelContext camelContext) {
        ClassLoader classLoader = camelContext != null && camelContext.getApplicationContextClassLoader() != null
                ? camelContext.getApplicationContextClassLoader()
                : JqFunctions.class.getClassLoader();
        return EnvironmentBuilder.withDefaultLoaders(Jackson3JsonProvider.getInstance(), Versions.JQ_1_7, classLoader);
    }

    /**
     * Adds the functions bound in the registry. A function bound as {@code name/arity} gets that arity, otherwise it is
     * variadic.
     */
    public static void loadFromRegistry(CamelContext camelContext, EnvironmentBuilder<JsonNode> builder) {
        Map<String, Function> fromRegistry = camelContext.getRegistry().findByTypeWithName(Function.class);
        if (fromRegistry != null) {
            LOGGER.debug("Loading {} jq functions from Registry", fromRegistry.size());
            fromRegistry.forEach((name, function) -> builder.defineFunction(signature(name), function));
        }
    }

    private static FunctionSignature signature(String name) {
        int pos = name.lastIndexOf('/');
        if (pos > 0) {
            return FunctionSignature.of(name.substring(0, pos), Integer.parseInt(name.substring(pos + 1)));
        }
        return FunctionSignature.ofVariadic(name);
    }

    private static <J> String asText(JsonProvider<J> provider, J node) {
        JsonNodeType type = provider.getNodeType(node);
        if (type == JsonNodeType.STRING) {
            return provider.getString(node);
        } else if (type == JsonNodeType.NULL) {
            return null;
        }
        return provider.format(node);
    }

    private static <J> void emitText(JsonProvider<J> provider, Output<J> output, String text) throws JsonQueryException {
        output.emit(text == null ? provider.createNull() : provider.createString(text), null);
    }

    public abstract static class ExchangeAwareFunction implements Function {
        @Override
        public <C extends RuntimeContext, J> Expression<C, J> bind(BindContext<J> bindCtx, List<Expression<C, J>> args)
                throws JsonQueryException {
            JsonProvider<J> provider = bindCtx.getJsonProvider();
            return (context, in, ipath, output) -> {
                Exchange exchange = EXCHANGE_LOCAL.get();
                if (exchange != null) {
                    doApply(provider, context, args, in, ipath, output, exchange);
                }
            };
        }

        protected abstract <C extends RuntimeContext, J> void doApply(
                JsonProvider<J> provider,
                C context,
                List<Expression<C, J>> args,
                J in,
                Path<J> ipath,
                Output<J> output,
                Exchange exchange)
                throws JsonQueryException;
    }

    /**
     * Evaluates the name argument, and the optional default value argument, and emits the value looked up with them.
     */
    public abstract static class NamedValueFunction extends ExchangeAwareFunction {
        @Override
        protected <C extends RuntimeContext, J> void doApply(
                JsonProvider<J> provider,
                C context,
                List<Expression<C, J>> args,
                J in,
                Path<J> ipath,
                Output<J> output,
                Exchange exchange)
                throws JsonQueryException {
            args.get(0).apply(context, in, ipath, (name, namePath) -> {
                if (args.size() == 2) {
                    args.get(1).apply(context, in, ipath, (defval, defvalPath) -> {
                        emitText(provider, output,
                                lookup(exchange, asText(provider, name), asText(provider, defval)));
                    });
                } else {
                    emitText(provider, output, lookup(exchange, asText(provider, name), null));
                }
            });
        }

        protected abstract String lookup(Exchange exchange, String name, String defaultValue);
    }

    /**
     * A function that allow to retrieve an {@link org.apache.camel.Message} header value as part of JQ expression
     * evaluation.
     *
     * As example, the following JQ expression sets the {@code .name} property to the value of the header named
     * {@code CommitterName}.
     *
     * <pre>
     * {@code
     * .name = header(\"CommitterName\")"
     * }
     * </pre>
     *
     */
    @FunctionRegistration(name = Header.NAME, nargs = 1)
    @FunctionRegistration(name = Header.NAME, nargs = 2)
    public static class Header extends NamedValueFunction {
        public static final String NAME = "header";

        @Override
        protected String lookup(Exchange exchange, String name, String defaultValue) {
            return exchange.getMessage().getHeader(name, defaultValue, String.class);
        }
    }

    /**
     * A function that allow to retrieve an {@link org.apache.camel.Message} property value as part of JQ expression
     * evaluation.
     *
     * As example, the following JQ expression sets the {@code .name} property to the value of the header named
     * {@code CommitterName}.
     *
     * <pre>
     * {@code
     * .name = property(\"CommitterName\")"
     * }
     * </pre>
     *
     */
    @FunctionRegistration(name = Property.NAME, nargs = 1)
    @FunctionRegistration(name = Property.NAME, nargs = 2)
    public static class Property extends NamedValueFunction {
        public static final String NAME = "property";

        @Override
        protected String lookup(Exchange exchange, String name, String defaultValue) {
            return exchange.getProperty(name, defaultValue, String.class);
        }
    }

    /**
     * A function that returns a constant value as part of JQ expression evaluation.
     *
     * As example, the following JQ expression sets the {@code .name} property to the constant value Donald.
     *
     * <pre>
     * {@code
     * .name = constant(\"Donald\")"
     * }
     * </pre>
     *
     */
    @FunctionRegistration(name = Constant.NAME, nargs = 1)
    @FunctionRegistration(name = Constant.NAME, nargs = 2)
    public static class Constant implements Function {
        public static final String NAME = "constant";

        @Override
        public <C extends RuntimeContext, J> Expression<C, J> bind(BindContext<J> bindCtx, List<Expression<C, J>> args)
                throws JsonQueryException {
            JsonProvider<J> provider = bindCtx.getJsonProvider();
            return (context, in, ipath, output) -> args.get(0).apply(context, in, ipath,
                    (value, path) -> emitText(provider, output, asText(provider, value)));
        }
    }

    /**
     * A function that allow to retrieve an {@link org.apache.camel.Exchange} variable value as part of JQ expression
     * evaluation.
     *
     * As example, the following JQ expression sets the {@code .name} property to the value of the variable named
     * {@code CommitterName}.
     *
     * <pre>
     * {@code
     * .name = variable(\"CommitterName\")"
     * }
     * </pre>
     *
     */
    @FunctionRegistration(name = Variable.NAME, nargs = 1)
    @FunctionRegistration(name = Variable.NAME, nargs = 2)
    public static class Variable extends NamedValueFunction {
        public static final String NAME = "variable";

        @Override
        protected String lookup(Exchange exchange, String name, String defaultValue) {
            return exchange.getVariable(name, defaultValue, String.class);
        }
    }

    /**
     * A function that returns the message body as part of JQ expression evaluation.
     *
     * As example, the following JQ expression sets the {@code .name} property to the message body.
     *
     * <pre>
     * {@code
     * .name = body()"
     * }
     * </pre>
     *
     */
    @FunctionRegistration(name = Body.NAME, nargs = 0)
    public static class Body implements Function {
        public static final String NAME = "body";

        @Override
        public <C extends RuntimeContext, J> Expression<C, J> bind(BindContext<J> bindCtx, List<Expression<C, J>> args)
                throws JsonQueryException {
            JsonProvider<J> provider = bindCtx.getJsonProvider();
            return (context, in, ipath, output) -> {
                Exchange exchange = EXCHANGE_LOCAL.get();
                if (exchange != null) {
                    // in case you refer to the body multiple times then we need to handle stream-caching
                    Object body = exchange.getMessage().getBody();
                    if (body instanceof StreamCache sc) {
                        sc.reset();
                    }
                    emitText(provider, output, exchange.getMessage().getBody(String.class));
                }
            };
        }
    }
}
