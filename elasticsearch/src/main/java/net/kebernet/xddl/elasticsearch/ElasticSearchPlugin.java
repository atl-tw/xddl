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
package net.kebernet.xddl.elasticsearch;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.CoreType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

public class ElasticSearchPlugin implements Plugin {
  private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
  private static final Map<CoreType, ObjectNode> DEFAULT_TYPES =
      Collections.unmodifiableMap(
          new HashMap<CoreType, ObjectNode>() {
            {
              try {
                put(
                    CoreType.INTEGER,
                    DEFAULT_MAPPER.readValue("{ \"type\":\"integer\" }", ObjectNode.class));

                put(
                    CoreType.LONG,
                    DEFAULT_MAPPER.readValue("{ \"type\":\"long\" }", ObjectNode.class));
                put(
                    CoreType.DOUBLE,
                    DEFAULT_MAPPER.readValue("{ \"type\":\"double\" }", ObjectNode.class));
                put(
                    CoreType.FLOAT,
                    DEFAULT_MAPPER.readValue("{ \"type\":\"float\" }", ObjectNode.class));
                put(
                    CoreType.BIG_INTEGER,
                    DEFAULT_MAPPER.readValue("{ \"type\":\"integer\" }", ObjectNode.class));
                put(
                    CoreType.BIG_DECIMAL,
                    DEFAULT_MAPPER.readValue(
                        "{ \"type\":\"scaled_float\", \"scaling_factor\":10000 }",
                        ObjectNode.class));
                put(
                    CoreType.BOOLEAN,
                    DEFAULT_MAPPER.readValue("{ \"type\":\"boolean\" }", ObjectNode.class));
                put(
                    CoreType.BINARY,
                    DEFAULT_MAPPER.readValue("{ \"type\":\"binary\" }", ObjectNode.class));
                put(
                    CoreType.DATE,
                    DEFAULT_MAPPER.readValue(
                        "{ \"type\":\"date\", \"format\":\"basic_date\" }", ObjectNode.class));
                put(
                    CoreType.TIME,
                    DEFAULT_MAPPER.readValue(
                        "{ \"type\":\"date\", \"format\":\"basic_time\" }", ObjectNode.class));
                put(
                    CoreType.DATETIME,
                    DEFAULT_MAPPER.readValue(
                        "{ \"type\":\"date\", \"format\":\"basic_date_time\" }", ObjectNode.class));
                put(
                    CoreType.STRING,
                    DEFAULT_MAPPER.readValue("{ \"type\":\"keyword\"}", ObjectNode.class));
                put(
                    CoreType.TEXT,
                    DEFAULT_MAPPER.readValue("{ \"type\":\"text\"}", ObjectNode.class));
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          });

  @Override
  public String getName() {
    return "elasticsearch";
  }

  @Override
  public String generateArtifacts(Context context, File outputDirectory) throws IOException {
    String filename =
        ofNullable(System.getProperty("elasticsearch.filename"))
            .orElseGet(
                () ->
                    ofNullable(context.getSpecification().getTitle())
                        .orElse("schema")
                        .replaceAll(" ", "_")
                        .toLowerCase());

    File outputFile = new File(outputDirectory, filename + ".mappings.json");

    try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile, false))) {
      LinkedHashSet<Structure> tree = new LinkedHashSet<>();
      Specification spec = context.getSpecification();
      BaseType type = context.getReferences().get(spec.getEntryRef());
      if (!(type instanceof Structure)) {
        throw context.stateException("Specification entry ref is not a structure.", null);
      }
      ObjectNode rootNode = context.getMapper().createObjectNode();
      ObjectNode defaultNode = context.getMapper().createObjectNode();
      ObjectNode mappingsNode = context.getMapper().createObjectNode();
      String indexName =
          ofNullable(System.getProperty("elasticsearch.indexname")).orElse(filename)
              + ofNullable(spec.getVersion()).map(s -> "-" + s).orElse("");
      defaultNode.set("dynamic", context.getMapper().valueToTree("strict"));
      mappingsNode.set("_default_", defaultNode);
      rootNode.set("mappings", mappingsNode);
      ObjectNode indexNode = visitStructure(tree, context, (Structure) type);
      mappingsNode.set(indexName, indexNode);
      rootNode.set(
          "settings",
          ofNullable(spec.ext().get("elasticsearch"))
              .orElseGet(context.getMapper()::createObjectNode));
      context.getMapper().writeValue(pw, rootNode);
    }
    return outputFile.getAbsolutePath();
  }

  private ObjectNode visitStructure(
      LinkedHashSet<Structure> tree, Context context, Structure type) {
    ObjectNode indexNode = context.getMapper().createObjectNode();
    ObjectNode properties = context.getMapper().createObjectNode();
    indexNode.set("properties", properties);
    tree.add(type);
    type.getProperties()
        .forEach(
            p -> {
              if (p instanceof Reference) {
                BaseType finalP = p;
                p =
                    context
                        .resolveReference((Reference) p)
                        .orElseThrow(() -> context.stateException("Unknown Reference", finalP));
              }
              if (p instanceof Structure) {
                ObjectNode structureNode = visitStructure(tree, context, (Structure) p);
                properties.set(p.getName(), structureNode);
                return;
              }
              if (p instanceof Type) {
                Type propType = (Type) p;
                ObjectNode propertyNode = doSimpleType(context, propType);
                properties.set(p.getName(), propertyNode);
                return;
              }
              if (p instanceof List) {
                List list = (List) p;
                BaseType listType = list.getContains();
                if (listType instanceof Reference) {
                  listType =
                      context
                          .resolveReference((Reference) listType)
                          .orElseThrow(
                              () -> context.stateException("Unable to resolve reference", list));
                }
                if (listType instanceof List) {
                  throw context.stateException(
                      "Elastic Search doesn't support lists of lists.", list);
                }

                if (listType instanceof Type) {
                  Type propType = (Type) listType;
                  ObjectNode nodeProperty = doSimpleType(context, propType);
                  ObjectNode ordered = context.getMapper().createObjectNode();
                  ordered.set("type", context.getMapper().valueToTree("nested"));
                  ordered.setAll(nodeProperty);
                  properties.set(p.getName(), ordered);
                  return;
                }
                if (listType instanceof Structure) {
                  if (tree.contains(listType)) {
                    throw context.stateException(
                        "Infinite cycle detected -- type contains list of itself,"
                            + " which is not supported in elastic search",
                        listType);
                  }
                  Structure listStruct = (Structure) listType;
                  ObjectNode property = visitStructure(tree, context, listStruct);
                  ObjectNode ordered = context.getMapper().createObjectNode();
                  ordered.set("type", context.getMapper().valueToTree("nested"));
                  ordered.setAll(property);
                  properties.set(p.getName(), ordered);
                  return;
                }
              }
            });
    tree.remove(type);
    return indexNode;
  }

  private ObjectNode doSimpleType(Context context, Type propType) {
    ObjectNode node =
        (ObjectNode)
            ofNullable(propType.ext().get("elasticsearch"))
                .orElse(DEFAULT_TYPES.get(propType.getCore()));
    if (node == null) {
      throw context.stateException("Unable to determine ElasticSearch config type", propType);
    }
    return node;
  }
}
