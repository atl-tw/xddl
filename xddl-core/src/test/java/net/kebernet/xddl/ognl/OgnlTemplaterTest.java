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
package net.kebernet.xddl.ognl;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.StructureTest;
import org.junit.Test;

public class OgnlTemplaterTest {

  @Test
  public void testTemplate() throws IOException {
    ObjectMapper mapper = Loader.mapper();
    Specification spec =
        mapper.readValue(
            StructureTest.class.getResourceAsStream("/template.json"), Specification.class);
    OgnlTemplater templater = new OgnlTemplater(spec, null);

    assertThat(templater.fillTemplate("${specification.title} v${specification.version}xxx"))
        .isEqualTo("Foo v1.0xxx");
  }

  @Test
  public void testNoOp() throws IOException {
    ObjectMapper mapper = Loader.mapper();
    Specification spec =
        mapper.readValue(
            StructureTest.class.getResourceAsStream("/template.json"), Specification.class);
    OgnlTemplater templater = new OgnlTemplater(spec, null);
    assertThat(templater.fillTemplate("xxx")).isEqualTo("xxx");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testTemplatesEverywhere() throws IOException {
    ObjectMapper mapper = Loader.mapper();
    Specification spec =
        mapper.readValue(
            OgnlTemplaterTest.class.getResourceAsStream("/prefix.json"), Specification.class);
    Map vals =
        Loader.mapper()
            .readValue(OgnlTemplaterTest.class.getResourceAsStream("/prefixVals.json"), Map.class);
    OgnlTemplater templater = new OgnlTemplater(spec, vals);
    templater.run();
    String value =
        CharStreams.toString(
            new InputStreamReader(
                OgnlTemplaterTest.class.getResourceAsStream("/prefixExpected.json")));
    assertThat(Loader.mapper().writeValueAsString(spec)).isEqualTo(value);
  }
}
