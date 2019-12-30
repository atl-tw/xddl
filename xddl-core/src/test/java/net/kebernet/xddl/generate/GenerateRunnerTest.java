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
package net.kebernet.xddl.generate;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import net.kebernet.xddl.model.BaseType;
import org.junit.Test;

public class GenerateRunnerTest {

  @Test
  public void testIncludes() throws IOException {
    GenerateRunner runner =
        GenerateRunner.builder()
            .includes(Collections.singletonList(new File("./src/test/resources/includes-pass")))
            .specificationFile(new File("./src/test/resources/empty.json"))
            .build();
    runner.run();
    Set<String> structs =
        runner.getContext().getSpecification().structures().stream()
            .map(BaseType::getName)
            .collect(Collectors.toSet());
    Set<String> types =
        runner.getContext().getSpecification().types().stream()
            .map(BaseType::getName)
            .collect(Collectors.toSet());

    assertThat(structs).containsAtLeast("Struct1", "Struct2");
    assertThat(types).contains("int_type");
  }

  @Test(expected = IllegalStateException.class)
  public void testIncludesFail() throws IOException {
    GenerateRunner runner =
        GenerateRunner.builder()
            .plugins(Collections.singletonList("foobar"))
            .includes(
                Collections.singletonList(new File("./src/test/resources/includes-reference")))
            .specificationFile(new File("./src/test/resources/empty.json"))
            .build();
    runner.run();
  }
}
