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
package net.kebernet.xddl.migrate.format;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.Test;

public class MixinTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testObjectMixin() throws JsonProcessingException {
    JsonNode target = mapper.readTree("{\"foo\":\"fooVal\", \"bar\":{ \"barChild\":\"barVal\"}}");
    JsonNode source = mapper.readTree("{\"baz\":\"bazValue\"}");
    JsonNode expected =
        mapper.readTree(
            "{\"foo\":\"fooVal\", \"bar\":{ \"barChild\":\"barVal\"},\"baz\":\"bazValue\"}}");
    Mixin.mix(target, source);
    assertThat(target).isEqualTo(expected);
  }

  @Test
  public void testArrayMixin() throws JsonProcessingException {
    JsonNode target = mapper.readTree("[1, 2, 3, 4]");
    JsonNode source = mapper.valueToTree(5);
    JsonNode expected = mapper.readTree("[1,2,3,4,5]");
    Mixin.mix(target, source);
    assertThat(target).isEqualTo(expected);
  }

  @Test
  public void testArrayArrayMixin() throws JsonProcessingException {
    JsonNode target = mapper.readTree("[1, 2, 3, 4]");
    JsonNode source = mapper.valueToTree(Arrays.asList(5, 6, 7));
    JsonNode expected = mapper.readTree("[1,2,3,4,5, 6,7]");
    Mixin.mix(target, source);
    assertThat(target).isEqualTo(expected);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testUnsupported() throws JsonProcessingException {
    JsonNode target = mapper.valueToTree("This is a test");
    JsonNode source = mapper.valueToTree(5);
    Mixin.mix(target, source);
  }
}
