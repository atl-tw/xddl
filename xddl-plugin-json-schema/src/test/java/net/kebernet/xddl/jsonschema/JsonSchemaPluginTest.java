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
package net.kebernet.xddl.jsonschema;

import static com.google.common.truth.Truth.assertThat;
import static net.kebernet.xddl.jsonschema.JsonSchemaPlugin.PATTERN_BIG_DECIMAL;
import static net.kebernet.xddl.jsonschema.JsonSchemaPlugin.PATTERN_BIG_INTEGER;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import net.kebernet.xddl.jsonschema.model.Definition;
import net.kebernet.xddl.jsonschema.model.Schema;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class JsonSchemaPluginTest {

  @Test
  public void basicTest() throws IOException {
    JsonSchemaPlugin instance = new JsonSchemaPlugin();
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            JsonSchemaPlugin.class.getResourceAsStream("/sample.json"), Specification.class);
    Context ctx = new Context(mapper, spec);
    File basicDir = new File("build/test/basic");
    //noinspection ResultOfMethodCallIgnored
    basicDir.getParentFile().mkdirs();
    instance.generateArtifacts(ctx, basicDir);
  }

  @Test
  public void testListType() throws IOException {
    JsonSchemaPlugin instance = new JsonSchemaPlugin();
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            JsonSchemaPlugin.class.getResourceAsStream("/list.json"), Specification.class);
    Context ctx = new Context(mapper, spec);
    Schema schema = instance.createSchema(ctx);
    assertThat(schema.getDefinitions()).containsKey("HasAnArray");
    Definition def = schema.getDefinitions().get("HasAnArray");
    assertThat(def.getDescription()).isEqualTo("Has an array of string");
    assertThat(def.getProperties()).containsKey("values");
    Definition values = def.getProperties().get("values");
    assertThat(values.getType()).isEqualTo("array");
    assertThat(values.getMinLength()).isEqualTo(1);
    assertThat(values.getMaxLength()).isEqualTo(2);
    assertThat(values.getItems().getMinLength()).isEqualTo(4);
    assertThat(values.getItems().getMaxLength()).isEqualTo(5);
    mapper.writeValue(new File("build/test/basic/list.json"), schema);
  }

  @Test
  public void testIntegerType() throws IOException {
    JsonSchemaPlugin instance = new JsonSchemaPlugin();
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            JsonSchemaPlugin.class.getResourceAsStream("/int-range.json"), Specification.class);
    Context ctx = new Context(mapper, spec);
    Schema schema = instance.createSchema(ctx);
    assertThat(schema.getDefinitions()).containsKey("HasRanges");
    Definition def = schema.getDefinitions().get("HasRanges");
    assertThat(def.getProperties()).containsKey("exclusiveValue");
    Definition exclusive = def.getProperties().get("exclusiveValue");
    assertThat(exclusive.getExclusiveMinimum()).isEqualTo(2);
    assertThat(exclusive.getExclusiveMaximum()).isEqualTo(3);

    assertThat(def.getProperties()).containsKey("nonExclusiveValue");
    Definition nonExclusive = def.getProperties().get("nonExclusiveValue");
    assertThat(nonExclusive.getMinimum()).isEqualTo(4);
    assertThat(nonExclusive.getMaximum()).isEqualTo(5);
  }

  @Test
  public void testStringType() throws IOException {
    JsonSchemaPlugin instance = new JsonSchemaPlugin();
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            JsonSchemaPlugin.class.getResourceAsStream("/strings.json"), Specification.class);
    Context ctx = new Context(mapper, spec);
    Schema schema = instance.createSchema(ctx);
    assertThat(schema.getDefinitions()).containsKey("Whatever");
    Definition def = schema.getDefinitions().get("Whatever");
    assertThat(def.getProperties().get("phoneNumberValue").getPattern())
        .isEqualTo("(\\d\\d\\d) \\d\\d\\d-\\d\\d\\d\\d$");
    assertThat(def.getProperties().get("lengthsValue").getMinLength()).isEqualTo(2);
    assertThat(def.getProperties().get("lengthsValue").getMaxLength()).isEqualTo(3);
    mapper.writeValue(new File("build/test/basic/strings.json"), schema);
  }

  @Test
  public void nestedStructure() throws IOException {
    JsonSchemaPlugin instance = new JsonSchemaPlugin();
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            JsonSchemaPlugin.class.getResourceAsStream("/nested-structure.json"),
            Specification.class);
    Context ctx = new Context(mapper, spec);
    Schema schema = instance.createSchema(ctx);
    mapper.writeValue(System.out, schema);
  }

  @Test
  public void testCoreTypeExtendedProperties() throws IOException {
    JsonSchemaPlugin instance = new JsonSchemaPlugin();
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            JsonSchemaPlugin.class.getResourceAsStream("/extended-core-types.json"),
            Specification.class);
    Context ctx = new Context(mapper, spec);
    Schema schema = instance.createSchema(ctx);
    Definition def = schema.getDefinitions().get("Test");
    assertThat(def.getProperties().get("timestamp").getType()).isEqualTo("string");
    assertThat(def.getProperties().get("timestamp").getFormat()).isEqualTo("date-time");
    assertThat(def.getProperties().get("date").getType()).isEqualTo("string");
    assertThat(def.getProperties().get("date").getFormat()).isEqualTo("date");
    assertThat(def.getProperties().get("time").getType()).isEqualTo("string");
    assertThat(def.getProperties().get("time").getFormat()).isEqualTo("time");
    assertThat(def.getProperties().get("bigint").getType()).isEqualTo("string");
    assertThat(def.getProperties().get("bigint").getPattern()).isEqualTo(PATTERN_BIG_INTEGER);
    assertThat(def.getProperties().get("bigdec").getType()).isEqualTo("string");
    assertThat(def.getProperties().get("bigdec").getPattern())
        .isEqualTo(JsonSchemaPlugin.PATTERN_BIG_DECIMAL);
    assertThat(def.getProperties().get("positiveInt").getFormat()).isEqualTo("^\\d*$");
    assertThat(def.getRequired()).contains("timestamp");
    assertThat(def.getRequired()).contains("date");
    assertThat(def.getRequired()).doesNotContain("time");
  }

  @Test
  public void testNumberPatterns() {
    assertThat("-123").matches(PATTERN_BIG_INTEGER);
    assertThat("-123").matches(PATTERN_BIG_DECIMAL);
    assertThat("42").matches(PATTERN_BIG_INTEGER);
    assertThat("42").matches(PATTERN_BIG_DECIMAL);
    assertThat("-42.").matches(PATTERN_BIG_DECIMAL);
    assertThat("44.33").matches(PATTERN_BIG_DECIMAL);
    assertThat("4 3 1").doesNotMatch(PATTERN_BIG_INTEGER);
    assertThat("4 3 1").doesNotMatch(PATTERN_BIG_DECIMAL);
    assertThat("42z").doesNotMatch(PATTERN_BIG_INTEGER);
    assertThat("42z").doesNotMatch(PATTERN_BIG_DECIMAL);
  }

  @Test
  public void testEnums() throws IOException {
    JsonSchemaPlugin instance = new JsonSchemaPlugin();
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            JsonSchemaPlugin.class.getResourceAsStream("/enum-value.json"), Specification.class);
    Context ctx = new Context(mapper, spec);
    Schema schema = instance.createSchema(ctx);
    mapper.writeValue(new File("build/test/basic/enum.json"), schema);
    Definition def = schema.getDefinitions().get("HasEnumValue");
    assertThat(def.getProperties().get("enumValue").getEnums()).isNotEmpty();
  }
}
