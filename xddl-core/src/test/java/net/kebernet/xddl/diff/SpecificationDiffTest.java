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
package net.kebernet.xddl.diff;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import net.kebernet.xddl.model.Specification;
import org.junit.Test;

public class SpecificationDiffTest {

  private static final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void identities() throws IOException {
    File left = new File("src/test/resources/diffEmpty/baseTypes.json");
    File right = new File("src/test/resources/diffMisMatch/baseTypes.json");
    Specification leftSpec = mapper.readValue(left, Specification.class);
    Specification rightSpec = mapper.readValue(right, Specification.class);
    SpecificationDiff diff = new SpecificationDiff(mapper, leftSpec, rightSpec);
    Set<SchemaElement> result = diff.diff();
    result.forEach(
        r -> {
          System.out.println("Type mismatch:");
          System.out.println("\texpected: " + diff.left(r.pathToDotNotation()));
          System.out.println("\tactual: " + diff.right(r.pathToDotNotation()));
        });
    assertThat(result).isNotEmpty();
    assertThat(result.iterator().next().pathToDotNotation()).isEqualTo("intProperty");
  }
}
