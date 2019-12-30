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
package net.kebernet.xddl.unify;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import org.junit.Test;

public class UnifyRunnerTest {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Test
  public void unifyTest() throws IOException {
    UnifyCommand command = UnifyCommand.builder().build();
    command.setInputFile(new File("src/test/resources/simplePatch/add-properties.xddl.json"));
    command.setIncludes(
        Collections.singletonList(new File("src/test/resources/simplePatchInclude")));
    command.setPatches(
        Collections.singletonList(new File("src/test/resources/simplePatchPatches")));
    File output = new File("build/simplePatch/foo.json");
    output.getParentFile().mkdirs();
    command.setOutputFile(output);
    UnifyRunner.builder().command(command).build().run();

    Specification patched = Loader.builder().main(output).build().read();

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

    assertThat(patched.structures().get(1).getName()).isEqualTo("Other");
  }
}
