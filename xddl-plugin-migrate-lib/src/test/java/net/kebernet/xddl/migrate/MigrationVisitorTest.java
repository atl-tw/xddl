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
package net.kebernet.xddl.migrate;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kebernet.xddl.migrate.format.CaseFormat;
import org.junit.Test;

public class MigrationVisitorTest {

  @Test
  public void testReadTree() {
    JsonNode read = MigrationVisitor.readTree("{\"test\":1}");
    assertThat(read.get("test").asInt()).isEqualTo(1);
  }

  @Test(expected = RuntimeException.class)
  public void testReadTreeFail() {
    JsonNode read = MigrationVisitor.readTree("{\"test:1}");
    assertThat(read.get("test").asInt()).isEqualTo(1);
  }

  @Test
  public void testCaseFormat() {
    JsonNode node = new ObjectMapper().valueToTree("thisIsATest");
    JsonNode result =
        MigrationVisitor.convertCase(CaseFormat.LOWER_CAMEL, CaseFormat.UPPER_SNAKE, node);
    assertThat(result.asText()).isEqualTo("THIS_IS_A_TEST");
  }
}
