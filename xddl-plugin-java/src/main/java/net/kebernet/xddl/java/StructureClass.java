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
package net.kebernet.xddl.java;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.java.Resolver.parse;
import static net.kebernet.xddl.java.Resolver.resolvePackageName;
import static net.kebernet.xddl.model.ModelUtil.firstOf;
import static net.kebernet.xddl.model.Utils.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.squareup.javapoet.AnnotationSpec;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import net.kebernet.xddl.java.annotation.Parser;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;

public class StructureClass implements Writable {

  private static final String NONE = "none";
  private static final String[] THIS_THAT = new String[] {"this", "that"};
  private static final String[] THAT_THIS = new String[] {"that", "this"};
  private final Context ctx;
  private final TypeSpec.Builder typeBuilder;
  private final String packageName;
  private final ClassName className;
  private TypeName comparableParameter;
  private LinkedHashSet<String> comparableProperties;
  private JavaExtension extension;

  @SuppressWarnings("WeakerAccess")
  public StructureClass(Context context, Structure structure, ClassName name) {
    this.ctx = context;
    this.packageName = resolvePackageName(context);
    this.className = ofNullable(name).orElse(ClassName.get(packageName, structure.getName()));
    this.typeBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);

    ifNotNullOrEmpty(structure.getDescription(), d -> typeBuilder.addJavadoc(d));

    this.extension =
        context
            .readPluginAs("java", structure, JavaExtension.class)
            .map(this::doExtension)
            .orElse(null);

    if (this.extension != null && !Strings.isNullOrEmpty(this.extension.getAnnotations())) {
      typeBuilder.addAnnotations(
          new Parser(ctx)
              .parse(new ArrayList<>(neverNull(extension.getImports())), extension.getAnnotations())
                  .stream()
                  .map(AnnotationSpec.Builder::build)
                  .collect(Collectors.toList()));
    }

    java.util.List<Pair<BaseType, FieldSpec>> allProperties =
        neverNull(structure.getProperties()).stream()
            .filter(t -> !(t instanceof PatchDelete))
            .map(this::doPropertyType)
            .peek(pair -> typeBuilder.addMethod(createGetter(pair.right)))
            .peek(pair -> typeBuilder.addMethod(createSetter(pair.right)))
            .peek(pair -> typeBuilder.addMethod(createBuilder(pair.right)))
            .peek(pair -> typeBuilder.addField(pair.right))
            .collect(Collectors.toList());

    generateEquals(allProperties);
    generateHashCode(allProperties);
    if (comparableParameter != null) {
      generateCompareTo(allProperties);
    }
  }

  @SuppressWarnings("ConstantConditions")
  private void generateCompareTo(java.util.List<Pair<BaseType, FieldSpec>> allProperties) {
    LinkedHashSet<String> includeProperties =
        ofNullable(this.comparableProperties)
            .orElseGet(
                () ->
                    allProperties.stream()
                        .map(p -> p.left.getName())
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
    ArrayList<String> comparison = new ArrayList<>();
    comparison.add("int result = 0");
    for (Pair<BaseType, FieldSpec> p : allProperties) {
      String[] order = null;
      if (includeProperties.contains(p.left.getName())) {
        order = THIS_THAT;
      } else if (includeProperties.contains("!" + p.left.getName())) {
        order = THAT_THIS;
      } else {
        continue;
      }
      String getter = createGetterName(p.right) + "()";
      comparison.add(
          "result = ((Comparable) "
              + order[0]
              + "."
              + getter
              + ").compareTo("
              + order[1]
              + "."
              + getter
              + ")");
    }
    comparison.add("if(result != 0) return result");
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder("compareTo")
            .returns(TypeName.INT)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ParameterSpec.builder(comparableParameter, "that").build());
    for (String statement : comparison) {
      methodBuilder = methodBuilder.addStatement(statement);
    }
    methodBuilder = methodBuilder.addStatement("return 0");
    typeBuilder.addMethod(methodBuilder.build());
  }

  private void generateEquals(java.util.List<Pair<BaseType, FieldSpec>> allProperties) {
    StringBuilder codeBlock = new StringBuilder("return \n");
    for (Pair<BaseType, FieldSpec> p : allProperties) {
      codeBlock.append("   java.util.Objects.equals(");
      wrapReference(codeBlock, p, "this").append(",");
      wrapReference(codeBlock, p, "that").append(") && \n   ");
    }
    codeBlock.append("   true");

    typeBuilder.addMethod(
        MethodSpec.methodBuilder("equals")
            .returns(TypeName.BOOLEAN)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(ParameterSpec.builder(TypeName.OBJECT, "o").build())
            .addStatement("if(!(o instanceof " + this.className.simpleName() + ")) return false")
            .addStatement(
                this.className.simpleName() + " that = (" + this.className.simpleName() + ") o")
            .addStatement(codeBlock.toString())
            .build());
  }

  private void generateHashCode(java.util.List<Pair<BaseType, FieldSpec>> allProperties) {
    StringBuilder codeBlock = new StringBuilder("return   java.util.Objects.hash(\n   ");
    for (Pair<BaseType, FieldSpec> p : allProperties) {
      wrapReference(codeBlock, p, "this").append(",\n   ");
    }
    codeBlock.append(" 0)");

    typeBuilder.addMethod(
        MethodSpec.methodBuilder("hashCode")
            .returns(TypeName.INT)
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addStatement(codeBlock.toString())
            .build());
  }

  private StringBuilder wrapReference(
      StringBuilder codeBlock, Pair<BaseType, FieldSpec> p, String reference) {
    String wrapper = null;
    if (p.left instanceof List && !NONE.equals(wrapper = resolveListEqualityType((List) p.left))) {
      codeBlock
          .append(reference)
          .append(".")
          .append(p.right.name)
          .append(" == null ? null : ")
          .append(" new ")
          .append(wrapper)
          .append("<>(");
    }
    codeBlock.append(reference).append(".").append(p.right.name);
    if (p.left instanceof List && !NONE.equals(wrapper)) {
      codeBlock.append(")");
    }
    return codeBlock;
  }

  private String resolveListEqualityType(List left) {
    return ofNullable(left.ext().get("java"))
        .map(
            n -> {
              try {
                return ctx.getMapper().treeToValue(n, JavaExtension.class);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .map(JavaExtension::getEqualsHashCodeWrapper)
        .orElse(ArrayList.class.getCanonicalName());
  }

  private JavaExtension doExtension(JavaExtension extension) {
    for (String iface : neverNull(extension.getImplementsList())) {
      TypeName name = parse(iface, packageName);
      if (name instanceof ParameterizedTypeName) {
        ParameterizedTypeName parameterizedTypeName = (ParameterizedTypeName) name;
        if (parameterizedTypeName.rawType.equals(ClassName.get(Comparable.class))) {
          this.comparableParameter = parameterizedTypeName.typeArguments.get(0);
        }
      }
      typeBuilder.addSuperinterface(name);
    }
    if (!neverNull(extension.getCompareToIncludeProperties()).isEmpty()) {
      this.comparableProperties = new LinkedHashSet<>(extension.getCompareToIncludeProperties());
    }
    return extension;
  }

  public StructureClass(Context context, Structure structure) {
    this(context, structure, null);
  }

  private TypeSpec.Builder builder() {
    return typeBuilder;
  }

  private String createGetterName(FieldSpec fieldSpec) {
    String prefix = fieldSpec.type == TypeName.BOOLEAN ? "is" : "get";
    String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldSpec.name);
    return prefix + name;
  }

  private MethodSpec createGetter(FieldSpec fieldSpec) {

    return MethodSpec.methodBuilder(createGetterName(fieldSpec))
        .addModifiers(Modifier.PUBLIC)
        .addJavadoc(fieldSpec.javadoc)
        .addJavadoc("\n@return the value\n")
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
        .addJavadoc("\n@param value the value\n")
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

  private Pair<BaseType, FieldSpec> doPropertyType(BaseType baseType) {
    FieldSpec result = null;
    BaseType resolvedType = baseType;
    if (baseType instanceof Reference) {
      resolvedType =
          ctx.resolveReference((Reference) baseType)
              .orElseThrow(() -> ctx.stateException("Unable to resolve reference", baseType));
    }
    if (resolvedType instanceof Structure & baseType instanceof Reference) {
      result = doReferenceTo(resolvedType, ((Reference) baseType).getRef());
    } else if (resolvedType instanceof Type) {
      Type type = (Type) resolvedType;
      result = isNullOrEmpty(type.getAllowable()) ? doType(type) : doEnum(baseType, type);
    } else if (resolvedType instanceof List) {
      List list = (List) resolvedType;
      result = doListType(list);
    } else if (resolvedType instanceof Structure) {
      String typeName =
          CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, resolvedType.getName()) + "Type";
      ClassName name = ClassName.get(packageName, className.simpleName(), typeName);
      StructureClass struct = new StructureClass(ctx, (Structure) resolvedType, name);
      TypeSpec type = struct.builder().addModifiers(Modifier.PUBLIC, Modifier.STATIC).build();
      typeBuilder.addType(type);
      result = FieldSpec.builder(name, resolvedType.getName(), Modifier.PRIVATE).build();
    }
    if (resolvedType.ext().get("java") != null) {
      try {
        JavaExtension je =
            ctx.getMapper()
                .treeToValue((JsonNode) resolvedType.ext().get("java"), JavaExtension.class);
        if (!Strings.isNullOrEmpty(je.getAnnotations())) {
          ArrayList<String> imports = new ArrayList<>();
          imports.addAll(neverNull(je.getImports()));
          imports.addAll(neverNull(this.extension.getImports()));
          result =
              result == null
                  ? null
                  : result
                      .toBuilder()
                      .addAnnotations(
                          new Parser(ctx)
                              .parse(imports, je.getAnnotations()).stream()
                                  .map(AnnotationSpec.Builder::build)
                                  .collect(Collectors.toList()))
                      .build();
        }

      } catch (JsonProcessingException e) {
        throw ctx.stateException("unabel to read ", resolvedType.ext().get("java"));
      }
    }
    if (result == null) {
      throw ctx.stateException("Unsupported type ", baseType);
    }

    Optional<String> defaultValue =
        firstOf(readInitializer(baseType), readInitializer(resolvedType));
    if (defaultValue.isPresent()) {
      result = result.toBuilder().initializer(defaultValue.get()).build();
    }
    return new Pair<>(resolvedType, result);
  }

  private Optional<String> readInitializer(BaseType type) {
    try {
      JavaExtension java =
          ctx.getMapper().treeToValue((JsonNode) type.ext().get("java"), JavaExtension.class);
      return Optional.ofNullable(java).map(JavaExtension::getInitializer);
    } catch (JsonProcessingException e) {
      throw ctx.stateException("Couldn't parse", type.ext().get("java"));
    }
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
    TypeName check = Resolver.resolveListType(this.ctx, listType);
    checkArgument(
        check instanceof ClassName,
        "The type parameter on List properties should not contain a parameter.");
    ClassName collectionType = (ClassName) check;
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
                      collectionType, ClassName.get(packageName, ((Reference) contains).getRef())),
                  listType.getName())
              .addModifiers(Modifier.PRIVATE);
      ifNotNullOrEmpty(contains.getDescription(), s -> builder.addJavadoc(escape(s)));
      ifNotNullOrEmpty(contains.getComment(), s -> builder.addJavadoc("Comment: " + escape(s)));
      return builder.build();
    }
    if (contains instanceof Type) {
      FieldSpec.Builder builder =
          FieldSpec.builder(
              ParameterizedTypeName.get(collectionType, Resolver.resolveType(ctx, (Type) contains)),
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

  static class Pair<L, R> {
    final L left;
    final R right;

    Pair(L left, R right) {
      this.left = left;
      this.right = right;
    }
  }
}
