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

import static net.kebernet.xddl.java.Resolver.resolvePackageName;
import static net.kebernet.xddl.model.Utils.*;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import javax.lang.model.element.Modifier;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;

public class StructureClass {

  private final Context ctx;
  private final Structure structure;
  private final TypeSpec.Builder typeBuilder;
  private final String packageName;
  private final ClassName className;

  public StructureClass(Context context, Structure structure) {
    this.ctx = context;
    this.structure = structure;
    this.packageName = resolvePackageName(context);
    this.className = ClassName.get(packageName, structure.getName());
    this.typeBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
    ifNotNullOrEmpty(structure.getDescription(), d -> typeBuilder.addJavadoc(d));

    neverNull(structure.getProperties()).stream()
        .map(this::doPropertyType)
        .peek(fieldSpec -> typeBuilder.addMethod(createGetter(fieldSpec)))
        .peek(fieldSpec -> typeBuilder.addMethod(createSetter(fieldSpec)))
        .peek(fieldSpec -> typeBuilder.addMethod(createBuilder(fieldSpec)))
        .forEach(typeBuilder::addField);
  }

  private MethodSpec createGetter(FieldSpec fieldSpec) {
    String prefix = fieldSpec.type == TypeName.BOOLEAN ? "is" : "get";
    String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldSpec.name);
    return MethodSpec.methodBuilder(prefix + name)
        .addModifiers(Modifier.PUBLIC)
        .addJavadoc(fieldSpec.javadoc)
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

    EnumClass enumClass = new EnumClass(ctx, ref, type);
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
    ifNotNullOrEmpty(type.getDescription(), s -> builder.addJavadoc(s + "\n"));
    ifNotNullOrEmpty(type.getComment(), s -> builder.addJavadoc("Comment: " + s + "\n"));
    return builder.build();
  }
}
