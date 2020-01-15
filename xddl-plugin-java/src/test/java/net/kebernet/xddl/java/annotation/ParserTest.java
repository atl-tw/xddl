/*
 * Copyright 2019, 2020 Robert Cooper, ThoughtWorks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kebernet.xddl.java.annotation;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.annotations.VisibleForTesting;
import com.squareup.javapoet.AnnotationSpec;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class ParserTest {

  @Test
  public void testSimple() {
    Parser parser = new Parser(new Context(Loader.mapper(), new Specification()));
    List<String> imports =
        Arrays.asList(
            Test.class.getCanonicalName(),
            VisibleForTesting.class.getCanonicalName(),
            JsonInclude.class.getCanonicalName(),
            JsonTypeInfo.class.getCanonicalName(),
            JsonProperty.class.getCanonicalName(),
            JsonSubTypes.class.getCanonicalName(),
            Id.class.getCanonicalName());

    List<AnnotationSpec.Builder> builders =
        parser.parse(
            imports,
            "@Test\n@VisibleForTesting\n@JsonInclude(JsonInclude.Include.NON_NULL)\n"
                + "@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = \"@type\")\n"
                + "@JsonProperty(\"foo\")"
                + "@JsonSubTypes({\n"
                + "  @JsonSubTypes.Type(value = Reference.class, name = \"Reference\"),\n"
                + "  @JsonSubTypes.Type(value = Structure.class, name = \"Structure\"),\n"
                + "  @JsonSubTypes.Type(value = Type.class, name = \"Type\"),\n"
                + "  @JsonSubTypes.Type(value = List.class, name = \"List\"),\n"
                + "  @JsonSubTypes.Type(value = PatchDelete.class, name = \"PATCH_DELETE\")\n"
                + "})\n"
                + "@Id");

    assertThat(builders.get(0).build().toString()).isEqualTo("@org.junit.Test");
    assertThat(builders.get(1).build().toString())
        .isEqualTo("@com.google.common.annotations.VisibleForTesting");
    // resolve default value reference
    System.out.println(builders.get(2).build().toString());
    assertThat(builders.get(2).build().toString())
        .isEqualTo(
            "@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)");
    // Resolve into array types
    assertThat(builders.get(3).build().toString())
        .isEqualTo(
            "@com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME, property = \"@type\")");
    // Resolve default value literal.
    assertThat(builders.get(4).build().toString())
        .isEqualTo("@com.fasterxml.jackson.annotation.JsonProperty(\"foo\")");
    // Resolve into sub-annotations
    assertThat(builders.get(5).build().toString())
        .isEqualTo(
            "@com.fasterxml.jackson.annotation.JsonSubTypes({ @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = Reference.class, name = \"Reference\"), @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = Structure.class, name = \"Structure\"), @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = Type.class, name = \"Type\"), @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = List.class, name = \"List\"), @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = PatchDelete.class, name = \"PATCH_DELETE\") })");
    // Check that "Id doesn't collide with com.fasterxml.jackson.annotation.JsonTypeInfo.Id
    assertThat(builders.get(6).build().toString()).isEqualTo("@javax.persistence.Id");
  }

  @Test
  public void testTemporal() {
    Parser parser = new Parser(new Context(Loader.mapper(), new Specification()));
    List<String> imports =
        Arrays.asList(Temporal.class.getCanonicalName(), TemporalType.class.getCanonicalName());

    List<AnnotationSpec.Builder> builders =
        parser.parse(imports, "@Temporal(TemporalType.TIMESTAMP)");
    assertThat(builders.get(0).build().toString())
        .isEqualTo("@javax.persistence.Temporal(javax.persistence.TemporalType.TIMESTAMP)");
  }
}
