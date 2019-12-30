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
package net.kebernet.xddl.model;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;

public class StructureTest {

  private ObjectMapper mapper;
  private Specification spec;

  @Before
  public void setup() throws IOException {
    mapper = new ObjectMapper();
    spec =
        mapper.readValue(
            StructureTest.class.getResourceAsStream("/sample.json"), Specification.class);
  }

  @Test
  public void testSimpleParse() throws IOException {

    Specification spec2 =
        mapper.readValue(
            StructureTest.class.getResourceAsStream("/sample.json"), Specification.class);
    assertThat(spec).isEqualTo(spec2);
    spec2.getStructures().stream().findFirst().ifPresent(s -> s.setDescription("FOO"));
    assertThat(spec).isNotEqualTo(spec2);
  }
}
