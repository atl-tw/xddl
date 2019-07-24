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
package net.kebernet.xddl.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A core type from which all other descend
 *
 * @param <T> The subtype reference used for the merge operation
 */
@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Reference.class, name = "Reference"),
  @JsonSubTypes.Type(value = Structure.class, name = "Structure"),
  @JsonSubTypes.Type(value = Type.class, name = "Type"),
  @JsonSubTypes.Type(value = List.class, name = "List")
})
public abstract class BaseType<T extends BaseType> {
  private String name;
  private String description;
  private Map<String, JsonNode> ext;
  private Boolean required;

  /**
   * Returns the ext map, creating it if it is null
   *
   * @return a map of strings to json nodes.
   */
  public Map<String, JsonNode> ext() {
    if (this.ext == null) {
      this.ext = new HashMap<>();
    }
    return this.ext;
  }

  /**
   * Merges the configuration specified in the reference with the configuration of this type into a
   * new object of the same type.
   *
   * @param reference The reference to resolve.
   * @return The new BaseType
   */
  public abstract T merge(Reference reference);
}
