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
package net.kebernet.xddl.integration;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import net.kebernet.xddl.Runner;
import org.junit.Test;

public class PluginTest {

  @Test
  public void jsonSchemaTestWithMain() throws IOException {
    Runner.main(
        "--input-file",
        "src/test/resources/int-range.json",
        "--output-directory",
        "build/test/schema-test/",
        "--format",
        "json");
    File file = new File("build/test/schema-test/schema.json");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readValue(file, JsonNode.class);
    assertThat(
            node.get("definitions")
                .get("HasRanges")
                .get("properties")
                .get("exclusiveValue")
                .get("exclusiveMinimum")
                .intValue())
        .isEqualTo(2);
  }

  @Test
  public void jsonSchemaTestWithBuilder() throws IOException {
    Runner.builder()
        .outputDirectory(new File("build/test/schema-test-2/"))
        .plugins(Arrays.asList("json"))
        .specificationFile(new File("src/test/resources/int-range.json"))
        .build()
        .run();

    File file = new File("build/test/schema-test-2/schema.json");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode node = mapper.readValue(file, JsonNode.class);
    assertThat(
            node.get("definitions")
                .get("HasRanges")
                .get("properties")
                .get("exclusiveValue")
                .get("exclusiveMinimum")
                .intValue())
        .isEqualTo(2);
  }
}
