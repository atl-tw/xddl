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
package net.kebernet.xddl.java;

import static net.kebernet.xddl.java.Resolver.parse;
import static net.kebernet.xddl.java.Resolver.resolvePackageName;
import static net.kebernet.xddl.model.Utils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import javax.lang.model.element.Modifier;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;

public class StructureClass implements Writable {

  private final Context ctx;
  private final TypeSpec.Builder typeBuilder;
  private final String packageName;
  private final ClassName className;

  public StructureClass(Context context, Structure structure, ClassName name) {
    this.ctx = context;
    this.packageName = resolvePackageName(context);
    this.className =
        Optional.ofNullable(name).orElse(ClassName.get(packageName, structure.getName()));
    this.typeBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
    ifNotNullOrEmpty(structure.getDescription(), d -> typeBuilder.addJavadoc(d));

    context.hasPlugin("java", structure, this::doExtension, (node) -> {});

    neverNull(structure.getProperties()).stream()
        .filter(t -> !(t instanceof PatchDelete))
        .map(this::doPropertyType)
        .peek(fieldSpec -> typeBuilder.addMethod(createGetter(fieldSpec)))
        .peek(fieldSpec -> typeBuilder.addMethod(createSetter(fieldSpec)))
        .peek(fieldSpec -> typeBuilder.addMethod(createBuilder(fieldSpec)))
        .forEach(typeBuilder::addField);
  }

  private void doExtension(JsonNode jsonNode) {
    if (jsonNode.has("implements")) {
      JsonNode impl = jsonNode.get("implements");
      for (JsonNode iface : impl) {
        ClassName name = parse(iface.asText(), packageName);
        typeBuilder.addSuperinterface(name);
      }
    }
  }

  public StructureClass(Context context, Structure structure) {
    this(context, structure, null);
  }

  public TypeSpec.Builder builder() {
    return typeBuilder;
  }

  private MethodSpec createGetter(FieldSpec fieldSpec) {
    String prefix = fieldSpec.type == TypeName.BOOLEAN ? "is" : "get";
    String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldSpec.name);
    return MethodSpec.methodBuilder(prefix + name)
        .addModifiers(Modifier.PUBLIC)
        .addJavadoc(fieldSpec.javadoc)
        .addJavadoc("@return the value\n")
        .returns(fieldSpec.type)
        .addCode("return this." + fieldSpec.name + ";\n")
        .build();
  }

  private MethodSpec createSetter(FieldSpec fieldSpec) {
    String prefix = "set";
    String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldSpec.name);
    return MethodSpec.methodBuilder(prefix + name)
        .addModifiers(Modifier.PUBLIC)
        .addJavadoc(fieldSpec.javadoc)
        .addJavadoc("@param value the value\n")
        .returns(TypeName.VOID)
        .addParameter(ParameterSpec.builder(fieldSpec.type, "value", Modifier.FINAL).build())
        .addCode("this." + fieldSpec.name + " = value;\n")
        .build();
  }

  private MethodSpec createBuilder(FieldSpec fieldSpec) {
    String name = fieldSpec.name;
    return MethodSpec.methodBuilder(name)
        .addModifiers(Modifier.PUBLIC)
        .returns(className)
        .addJavadoc("@param value the value \n@return this\n")
        .addParameter(ParameterSpec.builder(fieldSpec.type, "value", Modifier.FINAL).build())
        .addCode("this." + fieldSpec.name + " = value;\n" + "return this;\n")
        .build();
  }

  public void write(File directory) throws IOException {
    JavaFile file = JavaFile.builder(packageName, typeBuilder.build()).build();
    file.writeTo(directory);
  }

  private FieldSpec doPropertyType(BaseType baseType) {
    BaseType resolvedType = baseType;
    if (baseType instanceof Reference) {
      resolvedType =
          ctx.resolveReference((Reference) baseType)
              .orElseThrow(() -> ctx.stateException("Unable to resolve reference", baseType));
    }
    if (resolvedType instanceof Structure & baseType instanceof Reference) {
      return doReferenceTo(resolvedType, ((Reference) baseType).getRef());
    }
    if (resolvedType instanceof Type) {
      Type type = (Type) resolvedType;
      return isNullOrEmpty(type.getAllowable()) ? doType(type) : doEnum(baseType, type);
    }
    if (resolvedType instanceof List) {
      List list = (List) resolvedType;
      return doListType(list);
    }
    if (resolvedType instanceof Structure) {
      String typeName =
          CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, resolvedType.getName()) + "Type";
      ClassName name = ClassName.get(packageName, className.simpleName(), typeName);
      StructureClass struct = new StructureClass(ctx, (Structure) resolvedType, name);
      TypeSpec type = struct.builder().addModifiers(Modifier.PUBLIC, Modifier.STATIC).build();
      typeBuilder.addType(type);
      return FieldSpec.builder(name, resolvedType.getName(), Modifier.PRIVATE).build();
    }
    throw ctx.stateException("Unsupported type ", baseType);
  }

  private FieldSpec doReferenceTo(BaseType resolvedType, String referenceName) {
    return FieldSpec.builder(
            ClassName.get(packageName, referenceName), resolvedType.getName(), Modifier.PRIVATE)
        .build();
  }

  private FieldSpec doEnum(BaseType baseType, Type type) {
    Reference ref = null;
    if (baseType instanceof Reference) {
      ref = (Reference) baseType;
      if (ctx.findType(ref.getRef())
          .map(Type::getAllowable)
          .map(HashSet::new)
          .filter(set -> set.equals(new HashSet<>(type.getAllowable())))
          .isPresent()) {
        // the type is a reference and the allowables came from the reference.
        //noinspection OptionalGetWithoutIsPresent
        return doReferenceTo(
            type,
            CaseFormat.LOWER_UNDERSCORE.to(
                CaseFormat.UPPER_CAMEL, ctx.findType(ref.getRef()).get().getName()));
      }
    }
    // the type is a reference, but the allowable list is local, so we need to generate it
    // OR the type is 100% local. Either way, we need to generate a local type for it.

    EnumClass enumClass = new EnumClass(ctx, ref, type, className.simpleName());
    TypeSpec nested = enumClass.builder().addModifiers(Modifier.PUBLIC).build();
    typeBuilder.addType(nested);
    return FieldSpec.builder(
            ClassName.get(packageName, className.simpleName(), nested.name),
            type.getName(),
            Modifier.PRIVATE)
        .addJavadoc(neverNull(type.getDescription()))
        .build();
  }

  private FieldSpec doType(Type type) {
    FieldSpec.Builder builder =
        FieldSpec.builder(Resolver.resolveType(ctx, type), type.getName(), Modifier.PRIVATE);
    ifNotNullOrEmpty(type.getDescription(), s -> builder.addJavadoc(escape(s) + "\n"));
    ifNotNullOrEmpty(type.getComment(), s -> builder.addJavadoc("Comment: " + escape(s)));
    return builder.build();
  }

  static String escape(String value) {
    return neverNull(value).replaceAll("\\$", "$$") + "\n";
  }

  private FieldSpec doListType(List listType) {
    BaseType contains = listType.getContains();
    if (contains instanceof List) {
      throw ctx.stateException("Lists of Lists not supported", listType);
    }
    if (contains instanceof Reference && ctx.pointsToType((Reference) contains)) {
      contains = ctx.resolveReference((Reference) contains).get();
    } else if (contains instanceof Reference && ctx.pointsToStructure((Reference) contains)) {
      FieldSpec.Builder builder =
          FieldSpec.builder(
              ParameterizedTypeName.get(
                  ClassName.get(java.util.List.class),
                  ClassName.get(packageName, ((Reference) contains).getRef())),
              listType.getName());
      ifNotNullOrEmpty(contains.getDescription(), s -> builder.addJavadoc(escape(s)));
      ifNotNullOrEmpty(contains.getComment(), s -> builder.addJavadoc("Comment: " + escape(s)));
      return builder.build();
    }
    if (contains instanceof Type) {
      FieldSpec.Builder builder =
          FieldSpec.builder(
              ParameterizedTypeName.get(
                  ClassName.get(java.util.List.class), Resolver.resolveType(ctx, (Type) contains)),
              listType.getName());
      ifNotNullOrEmpty(contains.getDescription(), s -> builder.addJavadoc(escape(s)));
      ifNotNullOrEmpty(contains.getComment(), s -> builder.addJavadoc("Comment: " + escape(s)));
      return builder.build();
    }
    if (contains instanceof Structure) {
      String typeName =
          CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, listType.getName()) + "Type";
      ClassName nestedName =
          ClassName.get(
              this.className.packageName(),
              typeName,
              this.className.simpleNames().toArray(new String[0]));
      StructureClass struct = new StructureClass(ctx, (Structure) contains, nestedName);
      TypeSpec type = struct.builder().addModifiers(Modifier.PUBLIC, Modifier.STATIC).build();
      typeBuilder.addType(type);
      return FieldSpec.builder(
              ParameterizedTypeName.get(ClassName.get(java.util.List.class), nestedName),
              listType.getName(),
              Modifier.PRIVATE)
          .build();
    }
    throw ctx.stateException("Unsupported list, ", listType);
  }
}
