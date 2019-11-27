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

public interface JavaMigration {

  /**
   * Computes a new stage value for "fieldName" and returns it
   *
   * @param root the root object
   * @param local the local object
   * @param fieldName the field name on the local object
   * @param current the current value from prior stages
   * @return the new current value
   */
  JsonNode migrate(ObjectNode root, JsonNode local, String fieldName, JsonNode current);
}
