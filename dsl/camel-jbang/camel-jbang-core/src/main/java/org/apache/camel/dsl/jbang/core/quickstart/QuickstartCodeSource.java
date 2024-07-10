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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import freemarker.cache.ByteArrayTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static freemarker.template.Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX;
import static freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX;
import static freemarker.template.Configuration.VERSION_2_3_33;

public class QuickstartCodeSource {
    private static final Logger LOG = LoggerFactory.getLogger(QuickstartCodeSource.class);
    static final Configuration configuration;
    static final ByteArrayTemplateLoader templateLoader = new ByteArrayTemplateLoader();

    static {
        configuration = new Configuration(VERSION_2_3_33);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setLogTemplateExceptions(false);
        configuration.setFallbackOnNullLoopVariable(false);
        configuration.setTemplateLoader(templateLoader);
        configuration.setInterpolationSyntax(SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        configuration.setTagSyntax(SQUARE_BRACKET_TAG_SYNTAX);
    }

    private QuickstartCodeSource() {
    }

    public static void generate(
            QuickstartCodeTemplateLoader loader,
            String outputPath,
            Map<String, Object> model) {

        InputStream stream = loader.getStream();
        if (stream != null) {
            try {
                templateLoader.putTemplate(loader.getTemplateName(), stream.readAllBytes());

                Function<String, Boolean> dependencyIsPresent = searchString -> {
                    Set<String> deps = (Set<String>) model.get("dependencies");
                    return deps.stream().anyMatch(dep -> dep.contains(searchString));
                };
                model.put("dependencyIsPresent", dependencyIsPresent);

                Template template = configuration.getTemplate(loader.getTemplateName());
                Path sourceFilePath = Paths.get(outputPath);
                Path generatedSourcePath = sourceFilePath.resolve(loader.getOutputName());

                if (loader.getOutputName().equals("application.properties")) {
                    StringWriter out = new StringWriter();
                    template.process(model, out);

                    String content = out.toString();
                    if (Files.exists(generatedSourcePath)) {
                        content = "\n" + content;
                    }

                    Files.writeString(generatedSourcePath, content, StandardOpenOption.APPEND);
                } else {
                    template.process(model, Files.newBufferedWriter(generatedSourcePath));
                    if (Files.exists(sourceFilePath) && Files.size(sourceFilePath) == 0) {
                        // Remove if the generated source is empty.
                        Files.delete(sourceFilePath);
                    }
                }
            } catch (TemplateException | IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            LOG.warn("No template found for: {}", loader.getTemplateName());
        }
    }
}
