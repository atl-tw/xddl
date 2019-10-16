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
import static net.kebernet.xddl.model.Utils.isNullOrEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
import javax.lang.model.element.Modifier;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
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

    structure.getProperties().forEach(this::visit);
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

  private void visit(BaseType baseType) {
    BaseType resolvedType = baseType;
    if (baseType instanceof Reference) {
      resolvedType =
          ctx.resolveReference((Reference) baseType)
              .orElseThrow(() -> ctx.stateException("Unable to resolve reference", baseType));
    }
    doMigrationSteps(resolvedType);
    if (resolvedType instanceof Structure & baseType instanceof Reference) {
      doStructureReference((Structure) resolvedType);
    } else if (resolvedType instanceof Type) {
      Type type = (Type) resolvedType;
      if (isNullOrEmpty(type.getAllowable())) doType(type);
      else doEnum(baseType, type);
    } else if (resolvedType instanceof List) {
      List list = (List) resolvedType;
      doListType(list);
    } else if (resolvedType instanceof Structure) {
      StructureMigration migration =
          new StructureMigration(
              ctx,
              (Structure) resolvedType,
              ClassName.get(
                  this.className.packageName(),
                  structure.getName() + "_" + resolvedType.getName()));
      nested.add(migration);
      writeNested(resolvedType, migration.className);
    } else if (resolvedType instanceof PatchDelete) {
      doDelete(baseType);
    }
  }

  private void writeNested(BaseType type, ClassName className) {
    applyBuilder.beginControlFlow("if(local.has($S))", type.getName());
    applyBuilder.addStatement(
        "new $T().apply(root, ($T) local.get($S))", className, ObjectNode.class, type.getName());
    applyBuilder.endControlFlow();
  }

  private void doMigrationSteps(BaseType type) {
    if (!type.ext().containsKey("migration")) return;
    try {
      Migration migration =
          Loader.mapper().treeToValue((JsonNode) type.ext().get("migration"), Migration.class);
      migration.getGroups().forEach(s -> writeCodeBlock(type, s));
    } catch (JsonProcessingException e) {
      throw ctx.stateException("Unable to parse migration node: " + e.getMessage(), type);
    }
  }

  private void writeCodeBlock(BaseType type, StepGroup group) {
    if (group instanceof JsonPathGroup) {
      writeJsonPathSteps(type, (JsonPathGroup) group);
    }
  }

  private void writeJsonPathSteps(BaseType type, JsonPathGroup group) {
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
          "$T result_$L = $T.ofNullable($L)",
          ParameterizedTypeName.get(Optional.class, JsonNode.class),
          type.getName(),
          Optional.class,
          scope);
    }

    group.steps.forEach(
        step ->
            b.addStatement(
                "result_$L = result_$L.map(n-> $T.evaluateJsonPath(n, $S))",
                type.getName(),
                type.getName(),
                MigrationVisitor.class,
                step));

    b.addStatement("local.set($S, result_$L.orElse(null))", type.getName(), type.getName());
    applyBuilder.addCode(b.build());
  }

  private void doStructureReference(Structure resolvedType) {
    // throw new UnsupportedOperationException();
  }

  private void doDelete(BaseType baseType) {
    applyBuilder.addStatement("$L.remove($S)", LOCAL, baseType.getName());
  }

  private void doListType(List list) {
    // throw new UnsupportedOperationException();
  }

  private void doEnum(BaseType baseType, Type type) {
    // throw new UnsupportedOperationException();
  }

  private void doType(Type type) {
    // throw new UnsupportedOperationException();
  }
}
