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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * This is a visitor interface that is called to handle a migration from one type to another.
 */
public interface MigrationVisitor {

  Configuration JACKSON_JSON_NODE_CONFIGURATION =
      Configuration.builder()
          .mappingProvider(new JacksonMappingProvider())
          .jsonProvider(new JacksonJsonNodeJsonProvider())
          .build();

  static JsonNode evaluateJsonPath(JsonNode node, String expression) {
    return (JsonNode)
        JsonPath.using(JACKSON_JSON_NODE_CONFIGURATION)
            .parse(node)
            .read(JsonPath.compile(expression));
  }

  void apply(ObjectNode root, ObjectNode local);
}
