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
package net.kebernet.xddl.migrate;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.migrate.MigrationVisitor.evaluateJsonPath;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.java.Resolver;
import net.kebernet.xddl.javatestutils.JavaTestCompiler;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class StructureMigrationTest {

  @Test
  public void testPatchDelete()
      throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    File output = new File("build/test-gen/deleteOnly");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/deleteOnly.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();

    ObjectNode node =
        (ObjectNode)
            new ObjectMapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/deleteOnly.sample.json"));
    visitor.apply(node, node);
    assertThat(node.has("foo")).isFalse();
    assertThat(node.has("bar")).isFalse();
    assertThat(node.get("baz").asText()).isEqualTo("BAZ!");
  }

  @Test
  public void jsonPathTest() throws IOException {

    JsonNode node = Loader.mapper().readTree("{\"foo\":\"bar\", \"baz\":{ \"quux\":\"whatever\"}}");

    Optional<JsonNode> read =
        ofNullable(node)
            .map(n -> evaluateJsonPath(n, "$.baz"))
            .map(n -> evaluateJsonPath(n, "$.quux"));

    System.out.println(read);
  }

  @Test
  public void testSimpleCopy() throws Exception {
    File output = new File("build/test-gen/simpleCopy");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/simpleCopy.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/simpleCopy.sample.json"));
    visitor.apply(node, node);
    System.out.println(Loader.mapper().writeValueAsString(node));
    assertThat(node.get("bar").asText()).isEqualTo("quux");
    assertThat(node.get("foo").get("local").asText()).isEqualTo("q-x");
  }

  @Test
  public void testNameExample() throws Exception {
    File output = new File("build/test-gen/nameExample");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/nameExample.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Name").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/nameExample.sample.json"));
    visitor.apply(node, node);
    System.out.println(Loader.mapper().writeValueAsString(node));
  }

  @Test
  public void testListExpand() throws Exception {
    File output = new File("build/test-gen/listexpand");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/listexpand.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/listexpand.sample.json"));
    visitor.apply(node, node);
    ArrayNode array = (ArrayNode) node.get("list");
    List<String> values = Arrays.asList("a", "b", "c");
    for (int i = 0; i < values.size(); i++) {
      assertThat(values.get(i)).isEqualTo(array.get(i).get("value").asText());
    }
  }

  @Test
  public void testListAdapt() throws Exception {
    File output = new File("build/test-gen/listadapt");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/listadapt.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);
    writer = new StructureMigration(ctx, spec.structures().get(1), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/listadapt.sample.json"));
    visitor.apply(node, node);
    ArrayNode array = (ArrayNode) node.get("list");
    List<String> values = Arrays.asList("Robert", "Leslie");
    for (int i = 0; i < values.size(); i++) {
      assertThat(values.get(i)).isEqualTo(array.get(i).get("firstName").asText());
    }
  }

  @Test
  public void testListCopyAdapt() throws Exception {
    File output = new File("build/test-gen/listcopyadapt");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/listcopyadapt.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);
    writer = new StructureMigration(ctx, spec.structures().get(1), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/listcopyadapt.sample.json"));
    visitor.apply(node, node);
    ArrayNode array = (ArrayNode) node.get("list");
    List<String> values = Arrays.asList("Robert", "Leslie");
    for (int i = 0; i < values.size(); i++) {
      assertThat(values.get(i)).isEqualTo(array.get(i).get("firstName").asText());
    }
    System.out.println(node);
  }

  @Test
  public void testSelectToUnroll() throws Exception {
    File output = new File("build/test-gen/selectToUnroll");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/selectToUnroll.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);
    writer = new StructureMigration(ctx, spec.structures().get(1), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream(
                        "/selectToUnroll.sample.json"));
    visitor.apply(node, node);
    assertThat(node.get("person1").get("firstName").asText()).isEqualTo("Leslie");
    assertThat(node.get("person2").get("firstName").asText()).isEqualTo("Robert");
    System.out.println(node);
  }

  @Test
  public void testSelectToUnroll2() throws Exception {
    File output = new File("build/test-gen/selectToUnroll2");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/selectToUnroll2.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);
    writer = new StructureMigration(ctx, spec.structures().get(1), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream(
                        "/selectToUnroll2.sample.json"));
    visitor.apply(node, node);
    System.out.println(node);
    assertThat(node.get("person1").get("firstName").asText()).isEqualTo("Robert");
    assertThat(node.get("person2").get("firstName").asText()).isEqualTo("Leslie");
    assertThat(node.get("spy").get("firstName").asText()).isEqualTo("James");
    assertThat(node.get("person3")).isInstanceOf(NullNode.class);
  }

  @Test
  public void testSimpleMap() throws Exception {
    File output = new File("build/test-gen/testSimpleMap");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/testSimpleMap.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/testSimpleMap.sample.json"));
    visitor.apply(node, node);
    assertThat(node.get("bar").asText()).isEqualTo("baz");
  }

  @Test
  public void testMigrateListMembers() throws Exception {
    File output = new File("build/test-gen/listMemberMigrate");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/listMemberMigrate.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream(
                        "/listMemberMigrate.sample.json"));
    visitor.apply(node, node);
    assertThat(node.get("list").get(0).asText()).isEqualTo("Robert Cooper");
    assertThat(node.get("list").get(1).asText()).isEqualTo("Leslie Cooper");
  }

  @Test
  public void testMigrateListMemberLiteral() throws Exception {
    File output = new File("build/test-gen/listMemberLiterals");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/listMemberLiterals.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream(
                        "/listMemberLiterals.sample.json"));
    visitor.apply(node, node);
    assertThat(node.get("list").get(0).asText()).isEqualTo("0.2");
    assertThat(node.get("list").get(1).asText()).isEqualTo("0.2");
  }

  @Test
  public void testRename()
      throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    File output = new File("build/test-gen/rename");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/toGeoPoint.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);
    writer = new StructureMigration(ctx, spec.structures().get(1), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Address").newInstance();

    ObjectNode node =
        (ObjectNode)
            new ObjectMapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/toGeoPoint.sample.json"));
    visitor.apply(node, node);
    assertThat(node.has("location")).isTrue();
    assertThat(node.get("location").get("lat").asText()).isEqualTo("123");
    assertThat(node.get("location").get("lon").asText()).isEqualTo("234");
  }

  @Test
  public void testCase()
      throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    File output = new File("build/test-gen/caseMigration");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/caseMigration.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Thing").newInstance();

    ObjectNode node =
        (ObjectNode)
            new ObjectMapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/caseMigration.sample.json"));
    visitor.apply(node, node);
    assertThat(node.get("value").asText()).isEqualTo("THIS_IS_A_TEST");
  }

  @Test
  public void testInsertInto()
      throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    File output = new File("build/test-gen/insertInto");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/insertInto.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Thing").newInstance();

    ObjectNode node =
        (ObjectNode)
            new ObjectMapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/insertInto.sample.json"));
    visitor.apply(node, node);
    assertThat(node.get("value"))
        .isEqualTo(
            MigrationVisitor.mapper.readTree(
                "{\n"
                    + "                    \"foo\": \"bar\",\n"
                    + "                    \"newVal\": \"oldValue\"\n"
                    + "                  }"));
  }

  @Test
  public void testListMixin() throws Exception {
    File output = new File("build/test-gen/listMixinTest");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/listMixinTest.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream("/listMixinTest.sample.json"));
    visitor.apply(node, node);
    assertThat(node.get("list"))
        .isEqualTo(MigrationVisitor.readTree("[\"first\", \"second\", \"third\", \"mixedIn\"]"));

    node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream(
                        "/listMixinTestDefault.sample.json"));
    visitor.apply(node, node);
    assertThat(node.get("list")).isEqualTo(MigrationVisitor.readTree("[\"mixedIn\"]"));
  }

  @Test
  public void testObjectMixin() throws Exception {
    File output = new File("build/test-gen/objectMixinTest");
    output.mkdirs();
    Specification spec =
        Loader.builder()
            .main(new File("src/test/resources/objectMixinTest.xddl.json"))
            .scrubPatchesFromBaseline(false)
            .build()
            .read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureMigration writer = new StructureMigration(ctx, spec.structures().get(0), null);
    writer.write(output);

    String packageName = Resolver.resolvePackageName(ctx);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    MigrationVisitor visitor =
        (MigrationVisitor) loader.loadClass(packageName + ".migration.Foo").newInstance();
    ObjectNode node =
        (ObjectNode)
            Loader.mapper()
                .readTree(
                    StructureMigrationTest.class.getResourceAsStream(
                        "/objectMixinTest.sample.json"));
    visitor.apply(node, node);
    assertThat(node.get("mixed"))
        .isEqualTo(
            MigrationVisitor.readTree(
                "{"
                    + " \"foo\": \"fooValue\",\n"
                    + "  \"bar\": \"barValue\","
                    + "\"quux\": \"quuxValue\""
                    + "}"));
  }
}
