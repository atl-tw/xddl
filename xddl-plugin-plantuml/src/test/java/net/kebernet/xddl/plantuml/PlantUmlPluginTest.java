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
package net.kebernet.xddl.plantuml;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.CharStreams;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class PlantUmlPluginTest {

  @Test
  public void testSimpleWithEnum() throws IOException {
    Specification spec =
        Loader.mapper()
            .readValue(
                PlantUmlPluginTest.class.getResourceAsStream("/simpleTest.xddl.json"),
                Specification.class);
    Context context = new Context(Loader.mapper(), spec);
    PlantUmlPlugin instance = new PlantUmlPlugin();
    String result = instance.generateArtifacts(context, new File("build/test-gen/simpleTest"));
    assertThat(result).isEqualTo("OK");
    assertThat(
            CharStreams.toString(
                new FileReader(new File("build/test-gen/simpleTest/xddl_v1.puml"))))
        .isEqualTo(
            "@startuml\n"
                + "\n"
                + "enum SomeEnumType {\n"
                + "\tBAR\n"
                + "\tBAZ\n"
                + "\tQUUX\n"
                + "}\n"
                + "\n"
                + "Foo --  \"1\" SomeEnumType\n"
                + "\n"
                + "class Foo {\n"
                + "\tintProperty : INTEGER\n"
                + "\tsomeEnum : SomeEnumType\n\n"
                + "}\n"
                + "@enduml");
  }

  @Test
  public void testListOfBaseTypes() throws IOException {
    Specification spec =
        Loader.mapper()
            .readValue(
                PlantUmlPluginTest.class.getResourceAsStream("/listOfBaseTypes.xddl.json"),
                Specification.class);
    Context context = new Context(Loader.mapper(), spec);
    PlantUmlPlugin instance = new PlantUmlPlugin();
    String result = instance.generateArtifacts(context, new File("build/test-gen/listOfBaseTypes"));
    assertThat(result).isEqualTo("OK");
    assertThat(
            CharStreams.toString(
                new FileReader(new File("build/test-gen/listOfBaseTypes/xddl_v1.puml"))))
        .isEqualTo(
            "@startuml\n"
                + "\n"
                + "class Foo {\n"
                + "\tlistOfStrings : List<STRING>\n"
                + "\n"
                + "}\n"
                + "@enduml");
  }

  @Test
  public void referenceList() throws IOException {
    Specification spec =
        Loader.mapper()
            .readValue(
                PlantUmlPluginTest.class.getResourceAsStream("/referenceList.xddl.json"),
                Specification.class);
    Context context = new Context(Loader.mapper(), spec);
    PlantUmlPlugin instance = new PlantUmlPlugin();
    String result = instance.generateArtifacts(context, new File("build/test-gen/referenceList"));
    assertThat(result).isEqualTo("OK");
    assertThat(
            CharStreams.toString(
                new FileReader(
                    new File("build/test-gen/referenceList/Sample_Specification_v1.puml"))))
        .isEqualTo(
            "@startuml\n"
                + "\n"
                + "class Parent {\n"
                + "\tlistOfStrings : List<string_type>\n"
                + "\n"
                + "}\n"
                + "@enduml");
  }
}
