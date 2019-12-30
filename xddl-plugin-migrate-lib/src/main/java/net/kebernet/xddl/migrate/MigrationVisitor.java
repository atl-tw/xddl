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
package net.kebernet.xddl.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import net.kebernet.xddl.migrate.format.CaseFormat;

/** This is a visitor interface that is called to handle a migration from one type to another. */
public interface MigrationVisitor {
  ObjectMapper mapper = new ObjectMapper();
  Configuration JACKSON_JSON_NODE_CONFIGURATION =
      Configuration.builder()
          .mappingProvider(new JacksonMappingProvider())
          .jsonProvider(new JacksonJsonNodeJsonProvider())
          .build();

  ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

  static boolean nullish(JsonNode node) {
    return node == null || node instanceof NullNode;
  }

  static JsonNode readTree(String s) {
    try {
      return mapper.readTree(s);
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse value:\n" + s, e);
    }
  }

  static JsonNode convertCase(CaseFormat from, CaseFormat to, JsonNode value) {
    if (nullish(value)) {
      return value;
    }
    return mapper.valueToTree(from.to(to).apply(value.asText()));
  }

  /**
   * A utility method to evaluate a json path expression
   *
   * @param node The node to evaluate from
   * @param expression the expression
   * @return the node that results.
   */
  static JsonNode evaluateJsonPath(JsonNode node, String expression) {
    if (nullish(node)) {
      return node;
    }
    try {
      return (JsonNode)
          JsonPath.using(JACKSON_JSON_NODE_CONFIGURATION)
              .parse(node)
              .read(JsonPath.compile(expression));
    } catch (PathNotFoundException e) {
      return mapper.valueToTree(null);
    } catch (InvalidPathException e) {
      throw new IllegalArgumentException("Unable to parse:(" + expression + ")", e);
    }
  }

  @SuppressWarnings("unused")
  static void migrateArrayChildren(ObjectNode root, ArrayNode list, MigrationVisitor childVisitor) {
    for (int i = 0; i < list.size(); i++) {
      JsonNode indexedValue = list.get(i);
      ObjectNode current = null;
      if (indexedValue instanceof ObjectNode) {
        current = (ObjectNode) indexedValue;
        childVisitor.apply(root, current);
      } else {
        current = mapper.createObjectNode();
        current.set("_", indexedValue);
        childVisitor.apply(root, current);
        current.remove("_");
      }
      list.set(i, current);
    }
  }

  /**
   * Takes a text like-node and applies a regex replacement
   *
   * @param node the node to search
   * @param search the regular expression
   * @param replace the replacement expression
   * @return A new json node with the replaced value.
   */
  @SuppressWarnings("unused")
  static JsonNode evaluateRegexReplace(JsonNode node, String search, String replace) {
    if (nullish(node)) {
      return node;
    }
    Pattern pattern = PATTERN_CACHE.get(search);
    if (pattern == null) {
      pattern = Pattern.compile(search);
      if (PATTERN_CACHE.putIfAbsent(search, pattern) != null) {
        Logger.getAnonymousLogger().finest("Suspicious race to putIfAbsent");
      }
    }
    String result = pattern.matcher(node.asText()).replaceAll(replace);
    return mapper.valueToTree(result);
  }

  /**
   * THe main apply method. It will take the Root node of the document, and the local object you are
   * migrating, and apply all changes that need to be applied.
   *
   * @param root The root object of the document
   * @param local The local object within the root we are migrating.
   */
  void apply(ObjectNode root, JsonNode local);
}
