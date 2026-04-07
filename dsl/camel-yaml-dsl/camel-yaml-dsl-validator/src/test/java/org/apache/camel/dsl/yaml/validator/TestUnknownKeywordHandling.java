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
package org.apache.camel.dsl.yaml.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;

public class TestUnknownKeywordHandling {

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        System.out.println("=== Testing unknown keyword handling in draft-04 ===\n");

        // Load a schema with a fake unknown keyword
        JsonNode schemaWithUnknown = mapper.readTree(
                TestUnknownKeywordHandling.class.getResourceAsStream("/test-unknown-keyword.json"));

        var config = SchemaRegistryConfig.builder().build();
        var registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_4,
                builder -> builder.schemaRegistryConfig(config));

        var location = SchemaLocation.of("classpath:/test-unknown-keyword.json");

        System.out.println("Loading schema with 'fakeUnknownKeyword'...");
        var schema = registry.getSchema(location, schemaWithUnknown);

        // Validate a simple object
        JsonNode data = mapper.readTree("{\"name\": \"test\"}");
        var errors = schema.validate(data);

        System.out.println("Validation errors: " + errors.size());
        System.out.println("\nNow testing with 'deprecated' keyword...\n");

        // Load a schema with deprecated keyword
        JsonNode schemaWithDeprecated = mapper.readTree(
                "{\"$schema\": \"http://json-schema.org/draft-04/schema#\", " +
                                                        "\"type\": \"object\", " +
                                                        "\"properties\": {\"name\": {\"type\": \"string\", \"deprecated\": true}}}");

        var location2 = SchemaLocation.of("classpath:/test-deprecated.json");
        System.out.println("Loading schema with 'deprecated' keyword...");
        var schema2 = registry.getSchema(location2, schemaWithDeprecated);

        var errors2 = schema2.validate(data);
        System.out.println("Validation errors: " + errors2.size());

        System.out.println("\nConclusion:");
        System.out.println("If both schemas load without warnings, then json-schema-validator 2.0");
        System.out.println("either (a) ignores all unknown keywords, or (b) specifically handles 'deprecated'.");
        System.out.println("Check the console output above for any warnings or errors.");
    }
}
