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
package net.kebernet.xddl.migrate.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import net.kebernet.xddl.migrate.MigrationVisitor;

public class Template {

  private static final ObjectNode EMPTY = MigrationVisitor.mapper.createObjectNode();
  private final JsonNode value;

  public Template(JsonNode value) {
    this.value = value;
  }

  public JsonNode insertInto(JsonNode template) {
    if (template instanceof ArrayNode) {
      visitInsert(((ArrayNode) template));
    } else if (template instanceof ObjectNode) {
      visitInsert((ObjectNode) template);
    }
    return template;
  }

  private void visitInsert(ObjectNode template) {
    for (Iterator<String> keys = template.fieldNames(); keys.hasNext(); ) {
      String key = keys.next();
      if (EMPTY.equals(template.get(key))) {
        template.set(key, value);
      } else {
        insertInto(template.get(key));
      }
    }
  }

  private void visitInsert(ArrayNode template) {
    for (int i = 0; i < template.size(); i++) {
      if (EMPTY.equals(template.get(i))) {
        template.set(i, value);
      } else {
        insertInto(template.get(i));
      }
    }
  }
}
