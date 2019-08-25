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
package net.kebernet.xddl.jsonschema;

import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.model.ModelUtil.maybeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kebernet.xddl.jsonschema.model.Definition;
import net.kebernet.xddl.jsonschema.model.Schema;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.CoreType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.model.Value;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

public class JsonSchemaPlugin implements Plugin {
  static final String PATTERN_BIG_DECIMAL = "^-?\\d*(\\.\\d*)?$";
  static final String PATTERN_BIG_INTEGER = "^-?\\d*$";
  private static final String STRING = "string";
  private static final Map<CoreType, String> STRING_FORMATS =
      Collections.unmodifiableMap(
          new HashMap<CoreType, String>() {
            {
              put(CoreType.DATE, "date");
              put(CoreType.TIME, "time");
              put(CoreType.DATETIME, "date-time");
            }
          });
  private static final Map<CoreType, String> NUMBER_PATTERNS =
      Collections.unmodifiableMap(
          new HashMap<CoreType, String>() {
            {
              put(CoreType.BIG_DECIMAL, PATTERN_BIG_DECIMAL);
              put(CoreType.BIG_INTEGER, PATTERN_BIG_INTEGER);
            }
          });
  private static Map<CoreType, Function<JsonNode, ? extends Number>> CORE_NUMBER_READERS =
      Collections.unmodifiableMap(
          new HashMap<CoreType, Function<JsonNode, ? extends Number>>() {
            {
              put(CoreType.BIG_INTEGER, JsonNode::bigIntegerValue);
              put(CoreType.BIG_DECIMAL, (node) -> new BigDecimal(node.asText()));
              put(CoreType.INTEGER, JsonNode::intValue);
              put(CoreType.LONG, JsonNode::longValue);
              put(CoreType.FLOAT, JsonNode::floatValue);
              put(CoreType.DOUBLE, JsonNode::doubleValue);
            }
          });
  private static Map<CoreType, String> CORE_TYPES =
      Collections.unmodifiableMap(
          new HashMap<CoreType, String>() {
            {
              put(CoreType.BIG_DECIMAL, STRING);
              put(CoreType.BIG_INTEGER, STRING);
              put(CoreType.BOOLEAN, "boolean");
              put(CoreType.INTEGER, "integer");
              put(CoreType.LONG, "integer");
              put(CoreType.FLOAT, "number");
              put(CoreType.DOUBLE, "number");
              put(CoreType.DATETIME, STRING);
              put(CoreType.DATE, STRING);
              put(CoreType.TIME, STRING);
              put(CoreType.TEXT, STRING);
              put(CoreType.STRING, STRING);
              put(CoreType.BINARY, STRING);
            }
          });

  @Override
  public String getName() {
    return "json";
  }

  @Override
  public String generateArtifacts(Context context, File outputDirectory) throws IOException {
    File file = new File(outputDirectory, "schema.json");
    //noinspection ResultOfMethodCallIgnored
    file.getParentFile().mkdirs();

    ObjectMapper mapper = context.getMapper();
    mapper.writeValue(file, createSchema(context));
    return "fuck-all";
  }

  Schema createSchema(Context context) {
    final Schema schema = new Schema();

    schema.setRef(
        ofNullable(context.getSpecification().ext().get("json"))
            .map(n -> n.get("ref"))
            .map(JsonNode::asText)
            .orElse("#/definitions/" + context.getSpecification().getEntryRef()));
    context.getSpecification().getStructures().forEach(s -> this.visit(context, s, schema));
    return schema;
  }

  private void visit(Context context, Structure s, Schema schema) {
    Definition def = doStructure(context, s);
    schema.definitions().put(s.getName(), def);
  }

  private Definition visitBaseType(Context context, BaseType p) {
    if (p == null) {
      System.err.println("BASE TYPE IS NULL? WHY IS THAT?");
      return null;
    }
    if (p instanceof Reference) {
      return doReference(context, (Reference) p);
    }
    if (p instanceof List) {
      return doList(context, (List) p);
    }
    if (p instanceof Type) {
      return doType(context, (Type) p);
    }
    if (p instanceof Structure) {
      return doStructure(context, (Structure) p);
    }
    throw new IllegalArgumentException("Unknown base type " + p.getName());
  }

  private Definition doStructure(Context context, Structure s) {
    Definition def = new Definition();
    if (def.getRef() != null) {
      throw context.stateException("Can't have a reference as a stop level definition", s);
    }
    def.setTitle(s.getName());
    def.setDescription(s.getDescription());
    def.setType("object");
    s.getProperties().stream()
        .filter(p -> !(p instanceof PatchDelete))
        .forEach(
            p -> {
              def.properties().put(p.getName(), this.visitBaseType(context, p));
              if (Boolean.TRUE.equals(p.getRequired())) {
                def.required().add(p.getName());
              }
            });
    return def;
  }

  private Definition doList(Context context, List list) {
    Definition def = new Definition();
    def.setTitle(list.getName());
    def.setDescription(list.getDescription());
    def.setType("array");
    doBaseTypeExtensions(context, list, def);
    def.setItems(visitBaseType(context, list.getContains()));
    return def;
  }

  private Definition doReference(Context context, Reference reference) {
    if (context.pointsToStructure(reference)) {
      Definition def = new Definition();
      def.setRef("#/definitions/" + reference.getRef());
      return def;
    } else if (context.pointsToType(reference)) {
      //noinspection OptionalGetWithoutIsPresent
      return doType(context, (Type) context.resolveReference(reference).get());
    }
    throw context.stateException("Unable to resolve reference " + reference.getRef(), reference);
  }

  private Definition doType(Context context, Type type) {
    Definition definition = new Definition();
    definition.setTitle(type.getName());
    definition.setDescription(type.getDescription());
    doTypeExtensions(context, type, definition);
    return definition;
  }

  private void doTypeExtensions(Context context, Type type, Definition definition) {
    this.doBaseTypeExtensions(context, type, definition);
    context.hasPlugin(
        "json",
        type,
        jsonNode -> {
          maybeSet(
              definition::setType,
              jsonNode,
              "contains",
              JsonNode::asText,
              CORE_TYPES.get(type.getCore()));
          maybeSet(
              definition::setMinimum, jsonNode, "minimum", CORE_NUMBER_READERS.get(type.getCore()));
          maybeSet(
              definition::setExclusiveMinimum,
              jsonNode,
              "exclusiveMinimum",
              CORE_NUMBER_READERS.get(type.getCore()));
          maybeSet(
              definition::setMaximum, jsonNode, "maximum", CORE_NUMBER_READERS.get(type.getCore()));
          maybeSet(
              definition::setExclusiveMaximum,
              jsonNode,
              "exclusiveMaximum",
              CORE_NUMBER_READERS.get(type.getCore()));
        },
        schemaType -> definition.setType(CORE_TYPES.get(type.getCore())));

    doCoreTypeSupport(type, definition);
  }

  private void doCoreTypeSupport(Type type, Definition definition) {
    if (definition.getFormat() == null && STRING_FORMATS.containsKey(type.getCore())) {
      definition.setFormat(STRING_FORMATS.get(type.getCore()));
    }
    if (definition.getPattern() == null && NUMBER_PATTERNS.containsKey(type.getCore())) {
      definition.setPattern(NUMBER_PATTERNS.get(type.getCore()));
    }
    ofNullable(type.getAllowable())
        .map(Collection::stream)
        .ifPresent(s -> definition.setEnums(s.map(Value::getValue).collect(Collectors.toList())));
  }

  private void doBaseTypeExtensions(Context context, BaseType type, Definition definition) {
    context.hasPlugin(
        "json",
        type,
        jsonNode -> {
          maybeSet(definition::setFormat, jsonNode, "format", JsonNode::asText);
          maybeSet(
              definition::setAdditionalProperties,
              jsonNode,
              "additionalProperties",
              JsonNode::asBoolean);
          maybeSet(definition::setMinProperties, jsonNode, "minProperties", JsonNode::intValue);
          maybeSet(definition::setMaxProperties, jsonNode, "maxProperties", JsonNode::intValue);
          maybeSet(definition::setPattern, jsonNode, "pattern", JsonNode::asText);
          maybeSet(definition::setMinLength, jsonNode, "minLength", JsonNode::intValue);
          maybeSet(definition::setMaxLength, jsonNode, "maxLength", JsonNode::intValue);
        },
        (n) -> {});
  }
}
