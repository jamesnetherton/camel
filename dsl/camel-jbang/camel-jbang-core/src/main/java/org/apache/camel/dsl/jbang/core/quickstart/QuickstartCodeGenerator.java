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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.camel.main.download.DependencyDownloaderClassLoader;
import org.apache.camel.main.download.DownloadException;
import org.apache.camel.main.download.MavenDependencyDownloader;
import org.apache.camel.tooling.maven.MavenArtifact;

public final class QuickstartCodeGenerator {
    private static final String QUICKSTART_YAML = "META-INF/org/apache/camel/jbang/quickstart.yaml";
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private static final DependencyDownloaderClassLoader main = new DependencyDownloaderClassLoader(null);
    private final QuickstartCodeConfiguration configuration = new QuickstartCodeConfiguration();
    private final File applicationProperties;
    private final File resourcesDir;
    private final File sourceDir;
    private final File testDir;
    private final String packageName;
    private final Set<String> initialDependencies;

    public QuickstartCodeGenerator(
                                   File applicationProperties,
                                   File resourcesDir,
                                   File sourceDir,
                                   File testDir,
                                   String packageName,
                                   Set<String> initialDependencies) {

        this.applicationProperties = applicationProperties;
        this.resourcesDir = resourcesDir;
        this.sourceDir = sourceDir;
        this.testDir = testDir;
        this.packageName = packageName;
        this.initialDependencies = initialDependencies;
    }

    public void generateQuickstartCode() throws Exception {
        resolveQuickstartCodeConfigurations(this.initialDependencies);
        writeQuickstartCodeConfiguration();
        writeQuickstartSourceCode();
    }

    public QuickstartCodeConfiguration getConfiguration() {
        return configuration;
    }

    void resolveQuickstartCodeConfigurations(Set<String> dependencies) throws Exception {
        DependencyDownloaderClassLoader classLoader = new DependencyDownloaderClassLoader(null);
        try (MavenDependencyDownloader downloader = new MavenDependencyDownloader()) {
            downloader.setClassLoader(classLoader);
            downloader.start();
            for (String dependency : dependencies) {
                if (dependency.contains("org.apache.camel")) {
                    resolveQuickstartCodeConfiguration(downloader, classLoader, dependency);
                }
            }
        }
    }

    void resolveQuickstartCodeConfiguration(
            MavenDependencyDownloader downloader,
            DependencyDownloaderClassLoader classLoader,
            String dependency)
            throws Exception {

        String[] gavParts = dependency.replace("mvn:", "").split(":");
        if (gavParts.length != 3) {
            throw new IllegalArgumentException("Invalid Maven GAV: " + dependency);
        }

        MavenArtifact mavenArtifact = downloader.downloadArtifact(gavParts[0], gavParts[1], gavParts[2]);
        if (mavenArtifact != null) {
            classLoader.addFile(mavenArtifact.getFile());
            main.addFile(mavenArtifact.getFile());

            try (InputStream stream = classLoader.getResourceAsStream(QUICKSTART_YAML)) {
                if (stream != null) {
                    QuickstartCodeConfiguration dependencyQuickstartCodeConfiguration
                            = MAPPER.readValue(stream, QuickstartCodeConfiguration.class);
                    Set<String> dependencies = dependencyQuickstartCodeConfiguration.getDependencies();
                    if (!dependencies.isEmpty()) {
                        resolveQuickstartCodeConfigurations(dependencies);
                    }
                    configuration.combine(dependencyQuickstartCodeConfiguration, main);
                }
            }
        } else {
            throw new DownloadException("Cannot download artifact: " + dependency);
        }
    }

    void writeQuickstartCodeConfiguration() throws IOException {
        Path path = resourcesDir.toPath().resolve(applicationProperties.toPath());
        String result = configuration.getApplicationProperties()
                .entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
        Files.writeString(path, result, StandardOpenOption.APPEND);
    }

    void writeQuickstartSourceCode() {
        Set<String> dependencies = new HashSet<>();
        dependencies.addAll(initialDependencies);
        dependencies.addAll(configuration.getDependencies());

        Map<String, Object> model = new HashMap<>();
        model.put("package", packageName);
        model.put("dependencies", dependencies);

        // How can this work for test sources
        for (QuickstartCodeTemplateLoader templateLoader : configuration.getTemplateLoaders()) {
            QuickstartCodeSource.generate(templateLoader, getSourcePath(templateLoader), model);
        }
    }

    String getSourcePath(QuickstartCodeTemplateLoader templateLoader) {
        if (templateLoader.getTemplateName().contains("Test")) {
            return testDir.getPath();
        } else if (templateLoader.getTemplateName().startsWith("application.properties")) {
            return resourcesDir.getPath();
        } else {
            return sourceDir.getPath();
        }
    }
}
