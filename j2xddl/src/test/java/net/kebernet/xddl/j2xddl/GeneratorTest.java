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
package net.kebernet.xddl.j2xddl;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.kebernet.xddl.j2xddl.test.ListTest;
import net.kebernet.xddl.j2xddl.test.SimpleTest;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import org.junit.Test;

public class GeneratorTest {

  private final ObjectMapper mapper = new ObjectMapper();

  {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  @Test
  public void testSimpleClass() throws IOException {
    HashSet<Class> classes = new HashSet<>();
    classes.add(SimpleTest.class);
    Generator generator = new Generator(mapper, classes);
    Specification specification = generator.generate();
    Structure structure = specification.getStructures().get(0);
    assertThat(structure.getName()).isEqualTo("SimpleTest");
    assertThat(structure.getProperties().size()).isEqualTo(3);
    Set<String> names =
        structure.getProperties().stream().map(BaseType::getName).collect(Collectors.toSet());
    assertThat(names).containsExactly("field1", "field2", "field3");
    Set<String> required =
        structure.getProperties().stream()
            .filter(t -> Boolean.TRUE.equals(t.getRequired()))
            .map(BaseType::getName)
            .collect(Collectors.toSet());
    assertThat(required).containsExactly("field2");
    mapper.writeValue(new File("build/Simple.xddl.json"), specification);
  }

  @Test
  public void testList() throws IOException {
    HashSet<Class> classes = new HashSet<>();
    classes.add(ListTest.class);
    Generator generator = new Generator(mapper, classes);
    Specification specification = generator.generate();
    Structure structure = specification.getStructures().get(0);
    assertThat(structure.getName()).isEqualTo("ListTest");

    mapper.writeValue(new File("build/List.xddl.json"), specification);
  }
}
