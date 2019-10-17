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

import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.java.Resolver.resolvePackageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.lang.model.element.Modifier;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.plugins.Context;

public class StructureMigration {
  private static final String LOCAL = "local";
  public static final String ROOT = "root";
  private final Context ctx;
  private final Structure structure;
  private final String packageName;
  private final ClassName className;
  private final TypeSpec.Builder typeBuilder;
  private final MethodSpec.Builder applyBuilder;
  private final ArrayList<StructureMigration> nested = new ArrayList<>();

  public StructureMigration(Context context, Structure structure, ClassName name) {
    this.ctx = context;
    this.structure = structure;
    this.packageName = resolvePackageName(context) + ".migration";
    this.className = ofNullable(name).orElse(ClassName.get(packageName, structure.getName()));
    this.typeBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
    typeBuilder.addSuperinterface(MigrationVisitor.class);

    applyBuilder =
        MethodSpec.methodBuilder("apply")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ParameterSpec.builder(ObjectNode.class, ROOT).build())
            .addParameter(ParameterSpec.builder(ObjectNode.class, LOCAL).build());

    structure.getProperties().forEach(this::visitNested);
    structure.getProperties().forEach(this::visitStructureReference);
    structure.getProperties().forEach(this::visitMigrationSteps);
    structure.getProperties().forEach(this::visitPatchDelete);
  }

  private void visitPatchDelete(BaseType baseType) {
    if (baseType instanceof PatchDelete) {
      applyBuilder.addStatement(
          "if(local.has($S)) local.remove($S)", baseType.getName(), baseType.getName());
    }
  }

  private void visitLists(BaseType type) {
    if (type instanceof List) {
      List list = (List) type;
      BaseType baseType = list.getContains();
      applyBuilder.beginControlFlow(
          "if(local.has($S) && local.get($S) != null)", type.getName(), type.getName());
      applyBuilder.addStatement(
          "$T $L_list = ($T) local.get($S)",
          ArrayNode.class,
          type.getName(),
          ArrayNode.class,
          type.getName());
      applyBuilder.endControlFlow();

      MethodSpec.methodBuilder("migrate_list_" + list.getName())
          .addModifiers(Modifier.PUBLIC)
          .addParameter(ParameterSpec.builder(ObjectNode.class, ROOT).build())
          .addParameter(ParameterSpec.builder(ObjectNode.class, LOCAL).build())
          .addParameter(ParameterSpec.builder(JsonNode.class, "current").build());
    }
  }

  private void visitStructureReference(BaseType baseType) {
    BaseType resolvedType = baseType;
    if (baseType instanceof Reference) {
      resolvedType =
          ctx.resolveReference((Reference) baseType)
              .orElseThrow(() -> ctx.stateException("Unable to resolve reference", baseType));

      if (resolvedType instanceof Structure) {
        ClassName migrationName = ClassName.get(this.className.packageName(), structure.getName());
        writeNested(baseType, migrationName);
      }
    }
  }

  private void visitNested(BaseType baseType) {
    if (baseType instanceof Structure) {
      StructureMigration migration =
          new StructureMigration(
              ctx,
              (Structure) baseType,
              ClassName.get(
                  this.className.packageName(), structure.getName() + "_" + baseType.getName()));
      nested.add(migration);
      writeNested(baseType, migration.className);
    }
  }

  private void writeNested(BaseType type, ClassName className) {
    applyBuilder.beginControlFlow("if(local.has($S))", type.getName());
    applyBuilder.addStatement(
        "new $T().apply(root, ($T) local.get($S))", className, ObjectNode.class, type.getName());
    applyBuilder.endControlFlow();
  }

  public void write(File directory) {
    nested.forEach(n -> n.write(directory));
    typeBuilder.addMethod(applyBuilder.build());
    JavaFile file = JavaFile.builder(packageName, typeBuilder.build()).build();
    try {
      file.writeTo(directory);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void visitMigrationSteps(BaseType baseType) {
    BaseType resolvedType = baseType;
    if (baseType instanceof Reference) {
      resolvedType =
          ctx.resolveReference((Reference) baseType)
              .orElseThrow(() -> ctx.stateException("Unable to resolve reference", baseType));
    }
    doMigrationSteps(resolvedType);
  }

  private void doMigrationSteps(BaseType type) {
    if (!type.ext().containsKey("migration")) return;
    try {
      MethodSpec.Builder groupMethod =
          MethodSpec.methodBuilder("migrate_" + type.getName())
              .addModifiers(Modifier.PUBLIC)
              .addParameter(ParameterSpec.builder(ObjectNode.class, ROOT).build())
              .addParameter(ParameterSpec.builder(ObjectNode.class, LOCAL).build());
      groupMethod.addStatement(
          "$T current = local.has($S) ? local.get($S) : null",
          JsonNode.class,
          type.getName(),
          type.getName());

      Migration migration =
          Loader.mapper().treeToValue((JsonNode) type.ext().get("migration"), Migration.class);
      AtomicInteger index = new AtomicInteger(0);
      migration.getStages().forEach(s -> s.setIndex(index.getAndIncrement()));
      migration.getStages().forEach(s -> writeCodeBlock(type, s, groupMethod));
      groupMethod.addStatement("local.set($S, current)", type.getName());

      typeBuilder.addMethod(groupMethod.build());
      applyBuilder.addStatement("migrate_$L(root, local)", type.getName());
    } catch (JsonProcessingException e) {
      throw ctx.stateException("Unable to parse migration node: " + e.getMessage(), type);
    }
  }

  private void writeCodeBlock(BaseType type, Stage stage, MethodSpec.Builder groupsBuilder) {
    if (stage instanceof JsonPathStage) {
      writeJsonPathSteps(type, (JsonPathStage) stage, groupsBuilder);
    } else if (stage instanceof RegexStage) {
      writeRegExStage(type, (RegexStage) stage, groupsBuilder);
    }
  }

  private void writeRegExStage(BaseType type, RegexStage stage, MethodSpec.Builder groupsBuilder) {
    groupsBuilder.beginControlFlow("if(current != null)");
    groupsBuilder.addStatement(
        "current = $T.evaluateRegexReplace(current, $S, $S)",
        MigrationVisitor.class,
        escapeSlashes(stage.getSearch()),
        stage.getReplace());
    groupsBuilder.endControlFlow();
  }

  private String escapeSlashes(String search) {
    StringBuilder sb = new StringBuilder();
    for (char c : search.toCharArray()) {
      if (c == '\\') {
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }

  private void writeJsonPathSteps(
      BaseType type, JsonPathStage group, MethodSpec.Builder groupBuilder) {
    CodeBlock.Builder b = CodeBlock.builder();
    String scope = null;
    if (group.getStart() != null) {
      switch (group.getStart()) {
        case LOCAL:
          scope = LOCAL;
          break;
        default:
          scope = ROOT;
      }
      b.addStatement(
          "$T result = $T.ofNullable($L)",
          ParameterizedTypeName.get(Optional.class, JsonNode.class),
          Optional.class,
          scope);
    } else {
      b.addStatement(
          "$T result = $T.ofNullable(current)",
          ParameterizedTypeName.get(Optional.class, JsonNode.class),
          Optional.class);
    }

    group.steps.forEach(
        step ->
            b.addStatement(
                "result = result.map(n-> $T.evaluateJsonPath(n, $S))",
                MigrationVisitor.class,
                step));

    MethodSpec.Builder migrateMethod =
        MethodSpec.methodBuilder("migrate_" + type.getName() + "_" + group.getIndex())
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ParameterSpec.builder(ObjectNode.class, ROOT).build())
            .addParameter(ParameterSpec.builder(ObjectNode.class, LOCAL).build())
            .addParameter(ParameterSpec.builder(JsonNode.class, "current").build())
            .returns(JsonNode.class);

    migrateMethod.addCode(b.build());
    migrateMethod.addStatement("return result.orElse(null)");
    typeBuilder.addMethod(migrateMethod.build());
    groupBuilder.addStatement(
        "current = migrate_$L_$L(root, local, current)", type.getName(), group.getIndex());
  }
}
