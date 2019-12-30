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

import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.model.Utils.isNullOrEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.kebernet.xddl.migrate.format.CaseFormat;
import net.kebernet.xddl.model.*;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;
import net.kebernet.xddl.swift.model.Enum;
import net.kebernet.xddl.swift.model.LinesBuilder;
import net.kebernet.xddl.swift.model.Struct;
import net.kebernet.xddl.swift.model.SwiftType;

public class SwiftPlugin implements Plugin {

  private static HashMap<CoreType, String> SWIFT_TYPES =
      new HashMap<CoreType, String>() {
        {
          put(CoreType.BIG_DECIMAL, "Decimal");
          put(CoreType.BIG_INTEGER, "Decimal");
          put(CoreType.BINARY, "Data");
          put(CoreType.BOOLEAN, "Bool");
          put(CoreType.DATE, "Date");
          put(CoreType.DATETIME, "Date");
          put(CoreType.TIME, "Date");
          put(CoreType.DOUBLE, "Double");
          put(CoreType.FLOAT, "Float");
          put(CoreType.INTEGER, "Int");
          put(CoreType.LONG, "Int64");
          put(CoreType.STRING, "String");
          put(CoreType.TEXT, "String");
        }
      };

  private Context context;

  @Override
  public String getName() {
    return "swift";
  }

  @Override
  public String generateArtifacts(Context context, File outputDirectory) throws IOException {
    this.context = context;
    Specification spec = context.getSpecification();

    Optional<SpecificationExtension> ext =
        context.readPluginAs("swift", context.getSpecification(), SpecificationExtension.class);
    String libraryName =
        ext.map(SpecificationExtension::getLibraryName)
            .orElse(
                ofNullable(spec.getTitle())
                    .map(CaseFormat.UPPER_WORDS.to(CaseFormat.UPPER_CAMEL))
                    .orElse("xddl"));
    String version =
        ofNullable(spec.getVersion()).map(s -> "V" + s.replaceAll("\\.", "_")).orElse("");
    String sourcePath = libraryName + version;

    File sourcesDirectory = new File(outputDirectory, "Sources/" + sourcePath);

    spec.structures().stream().map(this::doStructure).forEach(writeTypeFile(sourcesDirectory));

    spec.types().stream()
        .filter(t -> !isNullOrEmpty(t.getAllowable()))
        .map(
            t -> {
              Optional<TypeExtension> extension =
                  context.readPluginAs("swift", t, TypeExtension.class);
              return buildEnum(
                  CaseFormat.LOWER_SNAKE.to(CaseFormat.UPPER_CAMEL).apply(t.getName()),
                  t.getAllowable(),
                  extension.orElse(null));
            })
        .forEach(writeTypeFile(sourcesDirectory));

    createPackageStructure(libraryName, outputDirectory);

    return "OK";
  }

  private Consumer<SwiftType> writeTypeFile(File sourcesDirectory) {
    return struct -> {
      try {
        writeFile(struct.getName() + ".swift", sourcesDirectory, struct.toString());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private void createPackageStructure(String libraryName, File outputDirectory) throws IOException {

    File[] files = new File(outputDirectory, "Sources").listFiles(File::isDirectory);
    java.util.List<File> targets = files == null ? Collections.emptyList() : Arrays.asList(files);
    java.util.List<String> targetNames =
        targets.stream().map(f -> "\"" + f.getName() + "\"").collect(Collectors.toList());

    LinesBuilder lb = new LinesBuilder();
    lb.append("// swift-tools-version:5.1")
        .append(
            "// The swift-tools-version declares the minimum version of Swift required to build this package.")
        .blank()
        .append("import PackageDescription")
        .blank()
        .append("let package = Package(")
        .indent()
        .append("name: $S,", libraryName)
        .append("products: [")
        .indent()
        .append(".library(")
        .indent()
        .append("name: $S,", libraryName)
        .append("targets: [$L]", Joiner.on(',').join(targetNames))
        .outdent()
        .append((")"))
        .outdent()
        .append("],")
        .append("dependencies: [],")
        .append("targets:[");
    targetNames.forEach(
        targetName -> {
          lb.indent()
              .append(".target(")
              .indent()
              .append("name: $L,", targetName)
              .append("dependencies:[]),")
              .outdent()
              .outdent();
        });
    lb.append("]");
    lb.outdent();
    lb.append(")");

    writeFile("Package.swift", outputDirectory, lb.toString());
    // let package =Package()
  }

  private void writeFile(String s, File outputDirectory, String content) throws IOException {
    if (!(outputDirectory.isDirectory() || outputDirectory.mkdirs())) {
      throw new IOException("Couldn't create directory " + outputDirectory.getAbsolutePath());
    }
    File target = new File(outputDirectory, s);
    try (OutputStreamWriter writer =
        new OutputStreamWriter(new FileOutputStream(target), Charsets.UTF_8)) {
      writer.append(content);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't write to " + target.getAbsolutePath(), e);
    }
  }

  private Struct doStructure(Structure structure) {
    Struct struct = new Struct();
    struct.setName(structure.getName());
    structure.getProperties().stream()
        .filter(p -> !(p instanceof PatchDelete))
        .map(p -> this.toField(struct, p))
        .forEach(struct.getFieldList()::add);
    boolean needsCoding =
        struct.getFieldList().stream().anyMatch(f -> f.getExtension().getFieldName() != null);
    if (needsCoding) {
      struct
          .getFieldList()
          .forEach(
              field -> {
                if (field.getExtension().getFieldName() != null) {
                  struct
                      .codingKeys()
                      .put(field.getExtension().getFieldName(), toTree(field.getName()));
                } else {
                  struct.codingKeys().put(field.getName(), toTree(null));
                }
              });
    }
    return struct;
  }

  private JsonNode toTree(Object value) {
    return context.getMapper().valueToTree(value);
  }

  private Struct.Field toField(Struct struct, BaseType<?> baseType) {
    PropertyExtension extension =
        baseType.ext().get("swift") != null ? parsePropertyExtension(baseType) : null;

    BaseType<?> resolved = baseType;

    if (resolved instanceof net.kebernet.xddl.model.List) {
      resolved = ((net.kebernet.xddl.model.List) baseType).getContains();
    }
    BaseType<?> finalType = resolved;
    if (resolved instanceof Reference) {
      finalType = context.resolve(resolved);
    }

    if (extension == null) {
      extension = resolved.ext().get("swift") != null ? parsePropertyExtension(baseType) : null;
    }
    if (finalType instanceof Type) {
      Type type = (Type) finalType;
      return doTypeField(struct, baseType, extension, resolved, type);
    } else if (finalType instanceof Structure) {
      Struct nested = doStructure((Structure) finalType);
      nested.setName(
          CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL).apply(baseType.getName()) + "Type");
      struct.addNestedType(nested);
      return new Struct.Field(
          baseType.getName(),
          nested.getName(),
          baseType.getRequired(),
          baseType instanceof List,
          extension);
    }

    throw context.stateException("Not sure how to handle ", baseType);
  }

  private Struct.Field doTypeField(
      Struct struct,
      BaseType<?> baseType,
      PropertyExtension extension,
      BaseType<?> resolved,
      Type type) {
    if (!isNullOrEmpty(type.getAllowable())) {
      if (resolved instanceof Reference) {
        return new Struct.Field(
            baseType.getName(),
            CaseFormat.LOWER_SNAKE
                .to(CaseFormat.UPPER_CAMEL)
                .apply(((Reference) resolved).getRef()),
            baseType.getRequired(),
            baseType instanceof List,
            extension);
      } else {
        Enum enumType =
            buildEnum(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL).apply(baseType.getName())
                    + "Type",
                type.getAllowable(),
                extension);
        struct.addNestedType(enumType);
        return new Struct.Field(
            baseType.getName(),
            enumType.getName(),
            baseType.getRequired(),
            baseType instanceof List,
            extension);
      }
    }

    return new Struct.Field(
        baseType.getName(),
        SWIFT_TYPES.get(type.getCore()),
        type.getRequired(),
        baseType instanceof List,
        extension);
  }

  private Enum buildEnum(String name, java.util.List<Value> allowable, TypeExtension extension) {
    return Enum.builder()
        .name(name)
        .nameValues(Enum.toNameValues(allowable))
        .valueType(Enum.determineEnumType(allowable))
        .build();
  }

  private PropertyExtension parsePropertyExtension(BaseType<?> baseType) {
    try {
      return context.getMapper().treeToValue(baseType.ext().get("swift"), PropertyExtension.class);
    } catch (JsonProcessingException e) {
      throw context.stateException("Unable to parse swift extension", baseType);
    }
  }
}
