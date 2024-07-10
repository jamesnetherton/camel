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

import java.io.InputStream;

public class QuickstartCodeTemplateLoader {
    public static final String CAMEL_JBANG_TEMPLATES = "META-INF/org/apache/camel/jbang/templates/";
    private final String templateName;
    private final String templatePath;
    private final String outputName;
    private final ClassLoader classLoader;

    QuickstartCodeTemplateLoader(String templateName, String language, ClassLoader classLoader) {
        this.templateName = templateName;
        this.outputName = QuickstartCodeHelper.getGeneratedSourceFileName(templateName, language);
        this.classLoader = classLoader;
        this.templatePath = CAMEL_JBANG_TEMPLATES + templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public String getOutputName() {
        return outputName;
    }

    InputStream getStream() {
        return classLoader.getResourceAsStream(templatePath);
    }
}
