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
package net.kebernet.xddl.hive;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.CoreType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

public class HivePlugin implements Plugin {

  private static final Map<CoreType, String> DATA_TYPES =
      Collections.unmodifiableMap(
          new HashMap<CoreType, String>() {
            {
              put(CoreType.LONG, "bigint");
              put(CoreType.INTEGER, "int");
              put(CoreType.BOOLEAN, "boolean");
              put(CoreType.DOUBLE, "double");
              put(CoreType.FLOAT, "float");
              put(CoreType.BINARY, "binary");
              put(CoreType.STRING, "varchar(255)");
              put(CoreType.TEXT, "varchar(65535)");
              put(CoreType.DATE, "date");
              put(CoreType.DATETIME, "timestamp");
              put(CoreType.TIME, "varchar(50)");
              put(CoreType.BIG_INTEGER, "DECIMAL(38,0)");
              put(CoreType.BIG_DECIMAL, "DECIMAL(34,4)");
            }
          });

  @Override
  public String getName() {
    return "hive";
  }

  @Override
  public String generateArtifacts(Context context, File outputDirectory) throws IOException {
    Specification spec = context.getSpecification();
    if (spec.getEntryRef() == null) {
      throw new IllegalArgumentException("EntryRef cannot be null");
    }
    JsonNode node =
        ofNullable(spec.ext().get("hive")).orElse(context.getMapper().createObjectNode());

    String tableName =
        node.has("table-name") ? node.get("table-name").asText() : context.createBaseFilename();
    String partitionedBy =
        node.has("partitioned-by")
            ? "PARTITIONED BY (" + node.get("partitioned-by").asText() + ") "
            : "";
    String location = node.has("location") ? node.get("location").asText() : "";

    Structure entryRef = context.entryRefStructure();

    java.util.List<String> fieldDefs =
        entryRef.getProperties().stream()
            .filter(p -> !(p instanceof PatchDelete))
            .map(t -> toField(context, t))
            .map(f -> f.name + " " + f.type)
            .collect(Collectors.toList());

    String tableSpec = Joiner.on(",\n  ").skipNulls().join(fieldDefs);

    MustacheFactory mf = new DefaultMustacheFactory();
    InputStream is = HivePlugin.class.getResourceAsStream("/baseTemplate.mustache");
    Mustache mustache = mf.compile(new InputStreamReader(is, Charsets.UTF_8), "baseTemplate");
    HashMap<String, Object> scopes = new HashMap<>();
    scopes.put("tableName", tableName);
    scopes.put("tableSpec", tableSpec);
    scopes.put("partitionedBy", partitionedBy);
    scopes.put("location", location);
    File outputFile = new File(outputDirectory, context.createBaseFilename() + ".hive");
    try (OutputStreamWriter writer =
        new OutputStreamWriter(new FileOutputStream(outputFile), Charsets.UTF_8)) {
      mustache.execute(writer, scopes);
    }
    return outputFile.getName();
  }

  private Field toField(Context context, BaseType property) {
    String name = property.getName();
    BaseType resolved = property;
    boolean isList = false;
    if (resolved instanceof Reference) {
      BaseType finalResolved = resolved;
      resolved =
          context
              .resolveReference((Reference) resolved)
              .orElseThrow(
                  () -> context.stateException("Unable to resolve reference", finalResolved));
    }

    if (property instanceof List) {
      resolved = ((List) property).getContains();
      isList = true;
    }

    if (resolved instanceof Reference) {
      BaseType finalResolved = resolved;
      resolved =
          context
              .resolveReference((Reference) resolved)
              .orElseThrow(
                  () -> context.stateException("Unable to resolve reference", finalResolved));
    }

    Field result = null;
    if (resolved instanceof Structure) {
      result = new Field(name, buildNestedStructure(context, (Structure) resolved));
    }
    if (resolved instanceof Type) {
      Type type = (Type) resolved;
      JsonNode node =
          ofNullable(type.ext().get("hive")).orElse(context.getMapper().createObjectNode());
      String typeName =
          node.has("type")
              ? node.get("type").asText()
              : ofNullable(DATA_TYPES.get(type.getCore()))
                  .orElseThrow(
                      () ->
                          context.stateException(
                              "Unable to find a hive datatype for element", type));
      result = new Field(name, typeName);
    }

    if (result == null) {
      throw context.stateException("Unable to build a hive definition for node", property);
    }
    if (isList) {
      return new Field(name, "ARRAY<" + result.type + ">");
    } else {
      return result;
    }
  }

  private String buildNestedStructure(Context context, Structure resolved) {
    return "STRUCT<"
        + Joiner.on(", ")
            .skipNulls()
            .join(
                resolved.getProperties().stream()
                    .filter(p -> !(p instanceof PatchDelete))
                    .map(t -> toField(context, t))
                    .map(f -> f.name + ":" + f.type)
                    .collect(Collectors.toList()))
        + ">";
  }

  private static class Field {
    final String name;
    final String type;

    private Field(String name, String type) {
      this.name = name;
      this.type = type;
    }
  }
}
