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

import static net.kebernet.xddl.java.StructureClass.escape;
import static net.kebernet.xddl.model.Utils.ifNotNullOrEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import javax.lang.model.element.Modifier;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.CoreType;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;

public class EnumClass implements Writable {
  private final BaseType base;
  private final Type resolved;
  private final String packageName;
  private final String parentClassName;

  public EnumClass(Context ctx, BaseType base, Type resolved, String parentClassName) {
    this.base = base;
    this.resolved = resolved;
    this.packageName = Resolver.resolvePackageName(ctx);
    this.parentClassName = parentClassName;
    if (resolved.getCore() != CoreType.STRING) {
      throw ctx.stateException(
          "Enum types are only supported for STRING core types right now",
          base != null ? base : resolved);
    }
  }

  TypeSpec.Builder builder() {
    String name =
        base != null
            ? CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, base.getName())
            : CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, resolved.getName()) + "Type";
    ClassName ref =
        parentClassName != null
            ? ClassName.get(packageName, parentClassName, name)
            : ClassName.get(packageName, name);
    TypeSpec.Builder builder =
        TypeSpec.enumBuilder(name)
            .addSuperinterface(Serializable.class)
            .addField(String.class, "value", Modifier.PRIVATE, Modifier.FINAL)
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addParameter(String.class, "value")
                    .addStatement("this.$N = $N", "value", "value")
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("toString")
                    .addAnnotation(Override.class)
                    .addAnnotation(JsonValue.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("return value")
                    .returns(String.class)
                    .build())
            .addMethod(
                MethodSpec.methodBuilder("forValue")
                    .addAnnotation(JsonCreator.class)
                    .addModifiers(Modifier.STATIC)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(String.class, "value")
                    .beginControlFlow("for ($T check : values())", ref)
                    .beginControlFlow("if(check.value.equals(value))")
                    .addStatement("return check")
                    .endControlFlow()
                    .endControlFlow()
                    .addStatement("throw new IllegalArgumentException(value)")
                    .returns(ref)
                    .build());
    resolved
        .getAllowable()
        .forEach(
            v -> {
              TypeSpec.Builder valueBuilder =
                  TypeSpec.anonymousClassBuilder("$S", v.getValue().asText());
              ifNotNullOrEmpty(v.getDescription(), (s) -> valueBuilder.addJavadoc(escape(s)));
              ifNotNullOrEmpty(
                  v.getComment(), (s) -> valueBuilder.addJavadoc("Comment: " + escape(s)));

              builder.addEnumConstant(
                  makeJavaNameFriendly(v.getValue().asText()), valueBuilder.build());
            });
    if (base != null) {
      ifNotNullOrEmpty(base.getDescription(), s -> builder.addJavadoc(escape(s)));
      ifNotNullOrEmpty(base.getComment(), s -> builder.addJavadoc("Comment: " + escape(s)));
    } else {
      ifNotNullOrEmpty(resolved.getDescription(), s -> builder.addJavadoc(escape(s)));
      ifNotNullOrEmpty(resolved.getComment(), s -> builder.addJavadoc("Comment: " + escape(s)));
    }
    return builder;
  }

  public void write(File directory) throws IOException {
    JavaFile file =
        JavaFile.builder(packageName, builder().addModifiers(Modifier.PUBLIC).build()).build();
    file.writeTo(directory);
  }

  private static String makeJavaNameFriendly(String value) {
    value = value.replaceAll("[-\\+]", "_");
    char c = value.charAt(0);
    if (Character.isAlphabetic(c) || c == '_') {
      return value;
    }
    return "VAL_" + value.toUpperCase();
  }
}
