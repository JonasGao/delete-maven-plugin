package com.github.jonasgao.delete;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Goal which touches a timestamp file.
 *
 * @goal touch
 * @phase process-sources
 */
@Mojo(name = "delete", threadSafe = true, defaultPhase = LifecyclePhase.VALIDATE)
public class DeleteMojo
        extends AbstractMojo {
    @Parameter(property = "project.file")
    private File pomFile;

    @Parameter(property = "deleteDependency", required = true)
    private String deleteDependency;

    public void execute()
            throws MojoExecutionException {
        String[] split = deleteDependency.split(":");
        String groupArtifactId = split[0] + ":" + split[1];
        String version;
        if (split.length > 2) {
            version = split[2];
        } else {
            version = null;
        }
        SAXBuilder builder = new SAXBuilder();
        Document document;
        try {
            document = builder.build(pomFile);
        } catch (JDOMException | IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        Element project = document.getRootElement();
        Namespace namespace = Namespace.getNamespace("http://maven.apache.org/POM/4.0.0");
        final Element dependencies = project.getChild("dependencies", namespace);
        dependencies.getChildren().removeIf(dependency -> {
            String depGroupId = dependency.getChildText("groupId", namespace);
            String depArtifactId = dependency.getChildText("artifactId", namespace);
            String depVersion = dependency.getChildText("version", namespace);
            if ((depGroupId + ":" + depArtifactId).equals(groupArtifactId)) {
                return version == null || version.equals(depVersion);
            }
            return false;
        });
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getCompactFormat());
        try (FileWriter writer = new FileWriter(pomFile)) {
            outputter.output(document, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
