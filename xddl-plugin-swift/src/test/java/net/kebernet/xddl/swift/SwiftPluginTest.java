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
package net.kebernet.xddl.swift;

import java.io.File;
import java.io.IOException;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class SwiftPluginTest {

  @Test
  public void testSimple() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder().main(new File("src/test/resources/SimpleTest.xddl.json")).build().read();
    File output = new File("build/swift/SimpleTest/");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }

  @Test
  public void testSimpleWithCoding() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/SimpleTestWithCodingKeys.xddl.json"))
            .build()
            .read();
    File output = new File("build/swift/SimpleTestWithCodingKey");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }

  @Test
  public void testSimpleWithVersion() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/SimpleTestWithVersion.xddl.json"))
            .build()
            .read();
    File output = new File("build/swift/SimpleTestWithVersion");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }

  @Test
  public void testSimpleWithTwoVersions() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/SimpleTestWithVersion1.xddl.json"))
            .build()
            .read();
    File output = new File("build/swift/SimpleTestWithTwoVersions");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    spec =
        Loader.builder()
            .main(new File("src/test/resources/SimpleTestWithVersion2.xddl.json"))
            .build()
            .read();
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }

  @Test
  public void nestedEnum() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder().main(new File("src/test/resources/nestedEnum.json")).build().read();
    File output = new File("build/swift/NestedEnum/");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }

  @Test
  public void nestedStructure() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder().main(new File("src/test/resources/nestedStructure.json")).build().read();
    File output = new File("build/swift/NestedStructure/");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }

  @Test
  public void enumReference() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder().main(new File("src/test/resources/enumReference.json")).build().read();
    File output = new File("build/swift/EnumReference/");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }

  @Test
  public void referenceList() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder().main(new File("src/test/resources/referenceList.json")).build().read();
    File output = new File("build/swift/referenceList");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }

  @Test
  public void referenceToStructureList() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/referenceToStructureList.json"))
            .build()
            .read();
    File output = new File("build/swift/referenceToStructureList");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }

  @Test
  public void structureReference() throws IOException, InterruptedException {
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/structureReference.json"))
            .build()
            .read();
    File output = new File("build/swift/structureReference");
    new SwiftPlugin().generateArtifacts(new Context(Loader.mapper(), spec), output);
    SwiftPM.buildDir(output);
  }
}
