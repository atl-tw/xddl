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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.File;
import java.io.IOException;
import javax.lang.model.element.Modifier;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;

public class StructureMigration {
  private static final String LOCAL = "local";
  private final Context ctx;
  private final Structure structure;
  private final String packageName;
  private final ClassName className;
  private final TypeSpec.Builder typeBuilder;
  private final MethodSpec.Builder applyBuilder;

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
            .addParameter(ParameterSpec.builder(ObjectNode.class, "document").build())
            .addParameter(ParameterSpec.builder(ObjectNode.class, LOCAL).build());

    structure.getProperties().forEach(this::visit);
  }

  public void write(File directory) throws IOException {
    typeBuilder.addMethod(applyBuilder.build());
    JavaFile file = JavaFile.builder(packageName, typeBuilder.build()).build();
    file.writeTo(directory);
  }

  private void visit(BaseType baseType) {
    BaseType resolvedType = baseType;
    if (baseType instanceof Reference) {
      resolvedType =
          ctx.resolveReference((Reference) baseType)
              .orElseThrow(() -> ctx.stateException("Unable to resolve reference", baseType));
    }
    if (resolvedType instanceof PatchDelete) {
      doDelete(baseType);
    } else if (resolvedType instanceof Structure & baseType instanceof Reference) {
      doStructureReference((Structure) resolvedType);
    } else if (resolvedType instanceof Type) {
      Type type = (Type) resolvedType;
      if (isNullOrEmpty(type.getAllowable())) doType(type);
      else doEnum(baseType, type);
    } else if (resolvedType instanceof List) {
      List list = (List) resolvedType;
      doListType(list);
    } else if (resolvedType instanceof Structure) {
      throw new UnsupportedOperationException();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private void doStructureReference(Structure resolvedType) {
    throw new UnsupportedOperationException();
  }

  private void doDelete(BaseType baseType) {
    applyBuilder.addStatement("$L.remove($S)", LOCAL, baseType.getName());
  }

  private void doListType(List list) {
    throw new UnsupportedOperationException();
  }

  private void doEnum(BaseType baseType, Type type) {
    throw new UnsupportedOperationException();
  }

  private void doType(Type type) {
    throw new UnsupportedOperationException();
  }
}
