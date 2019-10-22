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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/** This is a visitor interface that is called to handle a migration from one type to another. */
public interface MigrationVisitor {
  ObjectMapper mapper = new ObjectMapper();
  Configuration JACKSON_JSON_NODE_CONFIGURATION =
      Configuration.builder()
          .mappingProvider(new JacksonMappingProvider())
          .jsonProvider(new JacksonJsonNodeJsonProvider())
          .build();

  ConcurrentHashMap<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

  /**
   * A utility method to evaluate a json path expression
   *
   * @param node The node to evaluate from
   * @param expression the expression
   * @return the node that results.
   */
  static JsonNode evaluateJsonPath(JsonNode node, String expression) {
    return (JsonNode)
        JsonPath.using(JACKSON_JSON_NODE_CONFIGURATION)
            .parse(node)
            .read(JsonPath.compile(expression));
  }

  /**
   * Takes a text like-node and applies a regex replacement
   *
   * @param node the node to search
   * @param search the regular expression
   * @param replace the replacement expression
   * @return A new json node with the replaced value.
   */
  static JsonNode evaluateRegexReplace(JsonNode node, String search, String replace) {
    Pattern pattern = PATTERN_CACHE.get(search);
    if (pattern == null) {
      pattern = Pattern.compile(search);
      PATTERN_CACHE.put(search, pattern);
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