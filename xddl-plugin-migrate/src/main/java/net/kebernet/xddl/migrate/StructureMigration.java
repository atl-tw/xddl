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
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.lang.model.element.Modifier;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.migrate.format.CaseFormat;
import net.kebernet.xddl.migrate.format.Mixin;
import net.kebernet.xddl.migrate.format.Template;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.plugins.Context;

public class StructureMigration {
  private static final String LOCAL = "local";
  private static final String ROOT = "root";
  private static final String CURRENT = "current";
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
            .addParameter(ParameterSpec.builder(JsonNode.class, LOCAL).build());

    structure.getProperties().forEach(this::visitMigrationSteps);
    structure.getProperties().forEach(this::visitNested);
    structure.getProperties().forEach(this::visitStructureReference);
    structure.getProperties().forEach(this::visitLists);
    structure.getProperties().forEach(this::visitPatchDelete);
    typeBuilder.addField(
        FieldSpec.builder(
                this.className, "INSTANCE", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("new $T()", className)
            .build());
  }

  private void visitPatchDelete(BaseType baseType) {
    if (baseType instanceof PatchDelete) {
      applyBuilder.addStatement(
          "if(local.has($S)) (($T)local).remove($S)",
          baseType.getName(),
          ObjectNode.class,
          baseType.getName());
    }
  }

  private void visitLists(BaseType type) {
    if (type instanceof List) {
      List list = (List) type;
      BaseType baseType = list.getContains();
      applyBuilder.beginControlFlow(
          "if(local.has($S) && local.get($S) != null)", type.getName(), type.getName());
      BaseType resolvedType = baseType;
      if (baseType instanceof Reference || baseType instanceof Structure) {
        resolvedType =
            baseType instanceof Structure
                ? (Structure) baseType
                : ctx.resolveReference((Reference) baseType)
                    .orElseThrow(() -> ctx.stateException("Unable to resolve reference", baseType));
      }
      applyBuilder.addStatement(
          "$T $L_list = ($T) local.get($S)",
          ArrayNode.class,
          type.getName(),
          ArrayNode.class,
          type.getName());

      if (resolvedType instanceof Structure) {
        if (resolvedType.getName() == null) {
          resolvedType.setName(type.getName() + "Type");
        }
        visitListNested(resolvedType);
        applyBuilder.addStatement(
            "$T.migrateArrayChildren(root, $L_list, childVisitor)",
            MigrationVisitor.class,
            type.getName());
      } else if (resolvedType.ext().get("migration") != null) {
        applyBuilder.beginControlFlow("for(int i=0; i < $L_list.size(); i++)", type.getName());
        applyBuilder.addStatement(
            "$T indexedValue =  $L_list.get(i)", JsonNode.class, type.getName());
        applyBuilder.addStatement("$T current = null", ObjectNode.class);
        resolvedType.setName(type.getName() + "_member");
        doMigrationSteps(resolvedType, false);
        applyBuilder.addStatement("current = mapper.createObjectNode()", ObjectNode.class);
        applyBuilder.addStatement("current.set($S, indexedValue)", resolvedType.getName());
        applyBuilder.addStatement("migrate_$L(root, current)", resolvedType.getName());
        applyBuilder.addStatement(
            "$L_list.set(i, current.get($S))", type.getName(), resolvedType.getName());
        applyBuilder.endControlFlow();
      }
      applyBuilder.endControlFlow();
    }
  }

  private void visitStructureReference(BaseType baseType) {
    BaseType resolvedType;
    if (baseType instanceof Reference) {
      resolvedType =
          ctx.resolveReference((Reference) baseType)
              .orElseThrow(() -> ctx.stateException("Unable to resolve reference", baseType));

      if (resolvedType instanceof Structure) {
        ClassName migrationName =
            ClassName.get(this.className.packageName(), ((Reference) baseType).getRef());
        writeNested(baseType, migrationName);
      }
    }
  }

  private void visitListNested(BaseType baseType) {
    if (baseType instanceof Structure) {
      StructureMigration migration =
          new StructureMigration(
              ctx,
              (Structure) baseType,
              ClassName.get(
                  this.className.packageName(),
                  baseType.getName() != null
                      ? baseType.getName()
                      : structure.getName() + "_" + baseType.getName()));
      if (baseType.getName() != null) nested.add(migration);
      writeListNested(baseType.getName() == null ? className : migration.className);
    }
  }

  private void writeListNested(ClassName className) {
    applyBuilder.addStatement("$T childVisitor = $T.INSTANCE", MigrationVisitor.class, className);
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
    applyBuilder.beginControlFlow(
        "if(local.has($S) && !(local.get($S) instanceof $T))",
        type.getName(),
        type.getName(),
        NullNode.class);
    applyBuilder.addStatement(
        "$T.INSTANCE.apply(root, ($T) local.get($S))", className, ObjectNode.class, type.getName());
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
    doMigrationSteps(resolvedType, true);
  }

  private void doMigrationSteps(BaseType type, boolean apply) {
    if (!type.ext().containsKey("migration")) {
      return;
    }
    try {
      MethodSpec.Builder groupMethod =
          MethodSpec.methodBuilder("migrate_" + type.getName())
              .addModifiers(Modifier.PUBLIC)
              .addParameter(ParameterSpec.builder(ObjectNode.class, ROOT).build())
              .addParameter(ParameterSpec.builder(JsonNode.class, LOCAL).build());
      groupMethod.addStatement("String fieldName = $S", type.getName());
      groupMethod.addStatement(
          "$T current = local.has($S) ? local.get($S) : null",
          JsonNode.class,
          type.getName(),
          type.getName());

      Migration migration =
          Loader.mapper().treeToValue((JsonNode) type.ext().get("migration"), Migration.class);
      if (migration.getOp() == Migration.Operation.MIXIN) {
        groupMethod.addStatement(
            "$T original = $T.nullish(current) ? current : $T.readTree(current.toString())",
            JsonNode.class,
            MigrationVisitor.class,
            MigrationVisitor.class);
        if (migration.getDefaultMixinValue() != null
            && !(migration.getDefaultMixinValue() instanceof NullNode)) {
          groupMethod
              .beginControlFlow("if($T.nullish(original))", MigrationVisitor.class)
              .addStatement(
                  "original = $T.readTree($S)",
                  MigrationVisitor.class,
                  migration.getDefaultMixinValue().toString())
              .endControlFlow();
        }
      }
      AtomicInteger index = new AtomicInteger(0);
      migration.getStages().forEach(s -> s.setIndex(index.getAndIncrement()));
      migration.getStages().forEach(s -> writeCodeBlock(type, s, groupMethod));

      if (migration.getOp() == Migration.Operation.MIXIN) {
        groupMethod
            .addStatement("$T.mix(original, current)", Mixin.class)
            .addStatement("current = original");
      }
      groupMethod.addStatement("(($T) local).set(fieldName, current)", ObjectNode.class);

      typeBuilder.addMethod(groupMethod.build());
      if (apply) applyBuilder.addStatement("migrate_$L(root, local)", type.getName());
    } catch (JsonProcessingException e) {
      throw ctx.stateException("Unable to parse migration node: " + e.getMessage(), type);
    }
  }

  private void writeCodeBlock(BaseType type, Stage stage, MethodSpec.Builder groupsBuilder) {
    if (stage instanceof JsonPathStage) {
      writeJsonPathSteps(type, (JsonPathStage) stage, groupsBuilder);
    } else if (stage instanceof RegexStage) {
      writeRegExStage((RegexStage) stage, groupsBuilder);
    } else if (stage instanceof MapStage) {
      writeMapStage(type, (MapStage) stage, groupsBuilder);
    } else if (stage instanceof LiteralStage) {
      writeLiteralStage((LiteralStage) stage, groupsBuilder);
    } else if (stage instanceof RenameStage) {
      writeRenameState((RenameStage) stage, groupsBuilder);
    } else if (stage instanceof CaseStage) {
      writeCaseStage((CaseStage) stage, groupsBuilder);
    } else if (stage instanceof TemplateStage) {
      writeTemplateStage((TemplateStage) stage, groupsBuilder);
    } else if (stage instanceof JavaStage) {
      writeJavaStage((JavaStage) stage, groupsBuilder);
    }
  }

  private void writeJavaStage(JavaStage stage, MethodSpec.Builder groupsBuilder) {
    try {
      Class clazz = Class.forName(stage.getClassName());
      if (!JavaMigration.class.isAssignableFrom(clazz)) {
        throw ctx.stateException("Class is not a valid JavaMigration.", stage);
      }
      groupsBuilder.addStatement(
          "current = new $T().migrate(root, local, fieldName, current)", clazz);
    } catch (ClassNotFoundException e) {
      throw ctx.stateException("Could not find class from stage", stage);
    }
  }

  private void writeTemplateStage(TemplateStage stage, MethodSpec.Builder groupsBuilder) {
    groupsBuilder.addStatement(
        "current = new $T(current).insertInto($T.readTree($S))",
        Template.class,
        MigrationVisitor.class,
        stage.getInsertInto().toString());
  }

  private void writeCaseStage(CaseStage stage, MethodSpec.Builder groupsBuilder) {
    groupsBuilder.addStatement(
        "current = $T.convertCase($T.$L, $T.$L, current)",
        MigrationVisitor.class,
        CaseFormat.class,
        stage.getFrom().name(),
        CaseFormat.class,
        stage.getTo().name());
  }

  private void writeRenameState(RenameStage stage, MethodSpec.Builder groupsBuilder) {
    groupsBuilder.beginControlFlow("if(current.has($S))", stage.getFrom());
    groupsBuilder.addStatement(
        "(($T) current).set($S, current.get($S))",
        ObjectNode.class,
        stage.getTo(),
        stage.getFrom());
    groupsBuilder.addStatement("(($T) current).remove($S)", ObjectNode.class, stage.getFrom());
    groupsBuilder.endControlFlow();
  }

  private void writeLiteralStage(LiteralStage stage, MethodSpec.Builder groupsBuilder) {
    try {
      groupsBuilder.addStatement(
          "current = $T.readTree($S)",
          MigrationVisitor.class,
          MigrationVisitor.mapper.writeValueAsString(stage.getValue()));
    } catch (JsonProcessingException e) {
      throw ctx.stateException("Couldn't serialize ", stage.getValue());
    }
  }

  private void writeMapStage(BaseType type, MapStage stage, MethodSpec.Builder groupsBuilder) {

    String mapName = type.getName() + "_group_" + stage.getIndex();

    typeBuilder.addField(
        FieldSpec.builder(
                ParameterizedTypeName.get(HashMap.class, JsonNode.class, JsonNode.class),
                mapName,
                Modifier.STATIC,
                Modifier.FINAL)
            .initializer("new $T<>()", HashMap.class)
            .build());
    CodeBlock.Builder staticBlock = CodeBlock.builder();
    stage
        .getValues()
        .forEach(
            v -> {
              try {
                staticBlock.addStatement(
                    "$L.put( $T.readTree($S), $T.readTree($S))",
                    mapName,
                    MigrationVisitor.class,
                    ctx.getMapper().writeValueAsString(v.getFrom()),
                    MigrationVisitor.class,
                    ctx.getMapper().writeValueAsString(v.getTo()));
              } catch (JsonProcessingException e) {
                throw ctx.stateException("Couldn't serialize ", v);
              }
            });
    typeBuilder.addStaticBlock(staticBlock.build());

    groupsBuilder
        .beginControlFlow("if(current != null)")
        .addStatement(
            "current = $L.containsKey(current) ? $L.get(current) : current", mapName, mapName)
        .endControlFlow();
  }

  private void writeRegExStage(RegexStage stage, MethodSpec.Builder groupsBuilder) {
    groupsBuilder.addStatement(
        "current = $T.evaluateRegexReplace(current, $S, $S)",
        MigrationVisitor.class,
        escapeSlashes(stage.getSearch()),
        stage.getReplace());
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
    String scope;
    switch (group.getStart()) {
      case LOCAL:
        scope = LOCAL;
        break;
      case ROOT:
        scope = ROOT;
        break;
      default:
        scope = CURRENT;
    }
    b.addStatement(
        "$T result = $T.ofNullable($L)",
        ParameterizedTypeName.get(Optional.class, JsonNode.class),
        Optional.class,
        scope);

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
            .addParameter(ParameterSpec.builder(JsonNode.class, LOCAL).build())
            .addParameter(ParameterSpec.builder(JsonNode.class, CURRENT).build())
            .returns(JsonNode.class);

    migrateMethod.addCode(b.build());
    migrateMethod.addStatement("return result.orElse(null)");
    typeBuilder.addMethod(migrateMethod.build());
    groupBuilder.addStatement(
        "current = migrate_$L_$L(root, local, current)", type.getName(), group.getIndex());
  }
}
