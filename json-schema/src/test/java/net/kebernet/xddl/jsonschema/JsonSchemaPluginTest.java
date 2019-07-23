/*
 * Copyright 2019 Robert Cooper, ThoughtWorks
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
}
