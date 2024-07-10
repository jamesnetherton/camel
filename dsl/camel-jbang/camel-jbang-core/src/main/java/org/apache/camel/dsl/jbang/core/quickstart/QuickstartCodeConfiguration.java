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
package org.apache.camel.dsl.jbang.core.quickstart;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuickstartCodeConfiguration {
    private String language = "java";
    @JsonProperty("application-properties")
    private Map<String, String> applicationProperties = new HashMap<>();
    private Set<String> dependencies = new TreeSet<>();
    @JsonProperty("test-dependencies")
    private Set<String> testDependencies = new TreeSet<>();
    private Set<String> sources = new TreeSet<>();
    @JsonProperty("test-sources")
    private Set<String> testSources = new TreeSet<>();
    private Set<QuickstartCodeTemplateLoader> templateLoaders = new HashSet<>();

    public String getLanguage() {
        return language;
    }

    public Map<String, String> getApplicationProperties() {
        return applicationProperties;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public Set<String> getTestDependencies() {
        return testDependencies;
    }

    public Set<String> getSources() {
        return sources;
    }

    public Set<String> getTestSources() {
        return testSources;
    }

    public Set<QuickstartCodeTemplateLoader> getTemplateLoaders() {
        return templateLoaders;
    }

    public void combine(QuickstartCodeConfiguration other, ClassLoader classLoader) {
        applicationProperties.putAll(other.getApplicationProperties());
        dependencies.addAll(other.getDependencies());
        testDependencies.addAll(other.getTestDependencies());
        sources.addAll(other.getSources());
        testSources.addAll(other.getTestSources());

        other.getSources().forEach(template -> {
            templateLoaders.add(new QuickstartCodeTemplateLoader(template, other.getLanguage(), classLoader));
        });

        other.getTestSources().forEach(template -> {
            templateLoaders.add(new QuickstartCodeTemplateLoader(template, other.getLanguage(), classLoader));
        });
    }
}
