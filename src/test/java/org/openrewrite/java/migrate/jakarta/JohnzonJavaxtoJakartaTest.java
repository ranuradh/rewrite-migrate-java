/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class JohnzonJavaxtoJakartaTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JohnzonJavaxToJakarta")
        ).parser(JavaParser.fromJavaVersion().classpath("johnzon-core"));
    }

    @Test
    void migrateJohnzonDependencies() {
        rewriteRun(
          mavenProject("demo",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.example.ehcache</groupId>
                    <artifactId>johnzon-legacy</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <johnzon.version>1.2.5</johnzon.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.johnzon</groupId>
                            <artifactId>johnzon-core</artifactId>
                            <version>${johnzon.version}</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(actual -> {
                  assertThat(actual).isNotNull();
                  Matcher version = Pattern.compile("<johnzon.version>([0-9]+\\.[0-9]+\\.[0-9]+)</johnzon.version>")
                    .matcher(actual);

                  Matcher jsonApiVersion = Pattern.compile("2.1.\\d+").matcher(actual);
                  assertThat(jsonApiVersion.find()).describedAs("Expected jakarta.json-api 2.1.x version in %s", actual).isTrue();

                  assertThat(version.find()).isTrue();
                  return """
                <project>
                    <groupId>com.example.ehcache</groupId>
                    <artifactId>johnzon-legacy</artifactId>
                    <version>1.0.0</version>
                    <properties>
                        <johnzon.version>%s</johnzon.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.apache.johnzon</groupId>
                            <artifactId>johnzon-core</artifactId>
                            <version>${johnzon.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>jakarta.json</groupId>
                            <artifactId>jakarta.json-api</artifactId>
                            <version>%s</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """.formatted(version.group(1), jsonApiVersion.group(0));
              })
            ),
            srcMainJava(
              java(
                //language=java
                """
                package com.example.demo;
                
                import org.apache.johnzon.core.Snippet;
                
                public class A {
                    
                    void foo(Snippet snippet, String str) {
                    }
                }
                """
              )
            )
          )
        );
    }
}
