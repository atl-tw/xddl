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
package net.kebernet.xddl;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.CharStreams;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.stream.Collectors;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.ognl.OgnlTemplaterTest;
import org.junit.Test;

public class LoaderTest {

  @Test
  public void testPatchesDirectory() {
    Specification patched =
        Loader.builder()
            .main(new File("src/test/resources/simplePatch/add-properties.xddl.json"))
            .patches(Collections.singletonList(new File("src/test/resources/simplePatchPatches")))
            .build()
            .read();

    assertThat(
            patched.structures().stream()
                .findFirst()
                .map(Structure::getProperties)
                .map(
                    l -> {
                      return l.stream()
                          .filter(t -> !(t instanceof PatchDelete))
                          .map(BaseType::getName)
                          .collect(Collectors.toSet());
                    })
                .get())
        .containsExactly("property1", "property3", "child");
    assertThat(
            patched.structures().get(0).getProperties().stream()
                .filter(p -> "child".equals(p.getName()))
                .flatMap(
                    p ->
                        ((Structure) p)
                            .getProperties().stream().filter(t -> !(t instanceof PatchDelete)))
                .map(BaseType::getName)
                .collect(Collectors.toSet()))
        .containsExactly("property2", "property4");
  }

  @Test
  public void testTemplates() throws IOException {
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/prefix.json"))
            .valsFile(new File("src/test/resources/prefixVals.json"))
            .build()
            .read();
    String value =
        CharStreams.toString(
            new InputStreamReader(
                OgnlTemplaterTest.class.getResourceAsStream("/prefixExpected.json")));
    assertThat(Loader.mapper().writeValueAsString(spec)).isEqualTo(value);
  }
}
